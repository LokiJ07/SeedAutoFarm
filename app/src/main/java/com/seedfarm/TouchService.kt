package com.seedfarm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.*
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*

class TouchService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val wm get() = getSystemService(WINDOW_SERVICE) as WindowManager
    private val power get() = getSystemService(POWER_SERVICE) as PowerManager
    private var overlay: LinearLayout? = null
    private var status: TextView? = null
    private var active = ""
    private var activeWindow = -1
    private var target = ""
    private var targetWindow = -1
    private var bounds = Rect()
    private var rotation = 0
    private var steps = emptyList<Step>()
    private var index = 0
    private var generation = 0
    private var running = false
    private var paused = false
    private var deadline = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var lastScan = 0L
    private var guardNetwork = true
    private var guardScreen = true

    @Suppress("DEPRECATION")
    private fun screenRotation(): Int = wm.defaultDisplay.rotation

    private fun connected(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun scan(g: Int) {
        handler.postDelayed({ if(g == generation && running) stop("Screen check timed out") }, 5000)
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) { if(g==generation) stop("Screen check unavailable") }
                override fun onSuccess(result: ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val hardware = try { Bitmap.wrapHardwareBuffer(buffer,result.colorSpace) } finally { buffer.close() }
                    if(g!=generation || !running) { hardware?.recycle(); return }
                    val bitmap = try { hardware?.copy(Bitmap.Config.ARGB_8888,false) } finally { hardware?.recycle() }
                    if(bitmap==null) { stop("Screen check unavailable"); return }
                    recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener { resultText ->
                        if(g==generation && running) {
                            handler.removeCallbacksAndMessages(null)
                            val t = resultText.text.lowercase(java.util.Locale.ROOT)
                            if(listOf("disconnected","connection lost","lost connection","error code","failed to connect","reconnect","internet connection").any { t.contains(it) }) stop("Connection warning detected")
                            else { lastScan=SystemClock.elapsedRealtime(); tick(g) }
                        }
                    }.addOnFailureListener { if(g==generation) stop("Text check failed") }
                      .addOnCompleteListener { bitmap.recycle() }
                }
            })
        } catch(e:Exception) { stop("Screen check failed") }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD202020.toInt())
        }
        overlay = bar
        val params = WindowManager.LayoutParams(
            -2, -2, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        val handle = TextView(this).apply { text = "Drag controls"; setPadding(16,12,16,12) }
        bar.addView(handle)
        var ox = 0; var oy = 0; var sx = 0f; var sy = 0f
        handle.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { ox=params.x; oy=params.y; sx=event.rawX; sy=event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val screen = wm.currentWindowMetrics.bounds
                    params.x=(ox+(event.rawX-sx).toInt()).coerceIn(0,(screen.width()-bar.width).coerceAtLeast(0))
                    params.y=(oy+(event.rawY-sy).toInt()).coerceIn(0,(screen.height()-bar.height).coerceAtLeast(0))
                    wm.updateViewLayout(bar,params); true
                }
                MotionEvent.ACTION_UP -> { handle.performClick(); true }
                else -> false
            }
        }
        status = TextView(this).apply { text = "Stopped"; setPadding(12,8,12,8) }
        bar.addView(status)
        val buttons = LinearLayout(this)
        fun button(label: String, action: () -> Unit) {
            buttons.addView(Button(this).apply { text=label; setOnClickListener { action() } })
        }
        button("Start") { start() }
        button("Pause / Resume") { if(running) pause() else if(paused) resume() }
        button("Stop") { stop("Manual stop") }
        bar.addView(buttons)
        wm.addView(bar, params)
    }

    private fun unsafeReason(): String? {
        if (guardNetwork && !connected()) return "Network unavailable"
        if (!power.isInteractive || (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) return "Screen locked/off"
        if (power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) return "Device too hot"
        if ((getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) in 0..9) return "Low battery"
        if (active != target || activeWindow != targetWindow) return "Window changed"
        if (wm.currentWindowMetrics.bounds != bounds || screenRotation() != rotation) return "Display changed"
        if (SystemClock.elapsedRealtime() >= deadline) return "Timer expired"
        return null
    }

    private fun start() {
        if (running) return
        stop("Stopped")
        if (active.isBlank() || activeWindow < 0 || active == packageName || active.startsWith("com.android.") || active == "android") {
            stop("Open permitted target first"); return
        }
        try {
            val p = getSharedPreferences("routine",0)
            steps = parse(p.getString("points","50,60,2000")!!)
            val minutes = p.getString("minutes","0")!!.toLong()
            require(minutes in 0..720)
            deadline = if(minutes==0L) Long.MAX_VALUE else SystemClock.elapsedRealtime()+minutes*60000
            target=active; targetWindow=activeWindow
            bounds=Rect(wm.currentWindowMetrics.bounds); rotation=screenRotation()
            guardNetwork=p.getBoolean("networkGuard",true); guardScreen=p.getBoolean("screenGuard",true)
            lastScan=0L; index=0; running=true; status?.text="Running"
            tick(generation)
        } catch(e:Exception) { stop("Invalid setup") }
    }

    private fun pause() {
        generation++; running=false; paused=true
        handler.removeCallbacksAndMessages(null); status?.text="Paused"
    }
    private fun resume() {
        unsafeReason()?.let { stop(it); return }
        paused=false; running=true; lastScan=0L; status?.text="Running"; tick(generation)
    }
    private fun tick(g: Int) {
        if(g != generation || !running) return
        unsafeReason()?.let { stop(it); return }
        if(guardScreen && (lastScan==0L || SystemClock.elapsedRealtime()-lastScan>=2000)) { scan(g); return }
        val s=steps[index % steps.size]
        val x=bounds.left+bounds.width()*s.x/100
        val y=bounds.top+bounds.height()*s.y/100
        val location=IntArray(2)
        overlay?.let {
            it.getLocationOnScreen(location)
            if(x>=location[0] && x<location[0]+it.width && y>=location[1] && y<location[1]+it.height) {
                stop("Tap overlaps controls"); return
            }
        }
        val path=Path().apply { moveTo(x,y) }
        val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,80)).build()
        if(!dispatchGesture(gesture,object:GestureResultCallback(){
            override fun onCompleted(gd:GestureDescription) {
                if(g==generation && running) { index=(index+1)%steps.size; handler.postDelayed({tick(g)},s.delay) }
            }
            override fun onCancelled(gd:GestureDescription) { if(g==generation) stop("Gesture cancelled") }
        },handler)) stop("Gesture rejected")
    }

    private fun stop(reason:String) {
        generation++; running=false; paused=false; index=0
        handler.removeCallbacksAndMessages(null); status?.text=reason
    }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val p=e?.packageName?.toString() ?: return
        if(p==packageName) { if(running || paused) stop("Settings opened"); return }
        active=p; activeWindow=e.windowId
        if((running || paused) && (p!=target || activeWindow!=targetWindow)) stop("Window changed")
    }
    override fun onInterrupt() { stop("Interrupted") }
    override fun onDestroy() {
        stop("Service stopped")
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay=null; status=null
        runCatching { recognizer.close() }
        super.onDestroy()
    }
}
