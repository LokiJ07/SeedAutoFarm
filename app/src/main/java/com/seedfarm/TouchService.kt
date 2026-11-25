package com.seedfarm
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*
class TouchService:AccessibilityService(){
 private val h=Handler(Looper.getMainLooper());private var overlay:LinearLayout?=null;private var running=false;private var deadline=0L;private var steps=listOf<Step>();private var i=0;private var active="";private var target="";private var generation=0
 private val wm get()=getSystemService(WINDOW_SERVICE) as WindowManager
 override fun onServiceConnected(){super.onServiceConnected();val bar=LinearLayout(this);overlay=bar
 fun button(label:String,action:()->Unit){bar.addView(Button(this).apply{text=label;setOnClickListener{action()}})}
 button("Start"){start()};button("Pause"){stop()};button("Stop"){stop()}
 wm.addView(bar,WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END})
 }
 private fun start(){stop();if(active.isBlank() || active==packageName || active.startsWith("com.android.") || active=="android")return
 try{val p=getSharedPreferences("routine",0);steps=parse(p.getString("points","50,60,2000")!!);val minutes=p.getString("minutes","0")!!.toLong();require(minutes in 0..720);deadline=if(minutes==0L) Long.MAX_VALUE else SystemClock.elapsedRealtime()+minutes*60000;target=active;running=true;i=0;tick(generation)}catch(e:Exception){stop()}}
 private fun tick(g:Int){if(g!=generation)return;if(!running || (getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) in 0..9 || active!=target || SystemClock.elapsedRealtime()>=deadline || !(getSystemService(POWER_SERVICE) as PowerManager).isInteractive || (getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked){stop();return};val s=steps[i++%steps.size];val m=wm.currentWindowMetrics.bounds;val path=Path().apply{moveTo(m.width()*s.x/100,m.height()*s.y/100)};val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,80)).build();if(!dispatchGesture(gesture,object:GestureResultCallback(){override fun onCompleted(gd:GestureDescription){if(g==generation)h.postDelayed({tick(g)},s.delay)};override fun onCancelled(gd:GestureDescription){if(g==generation)stop()}},h))stop()}
 private fun stop(){generation++;running=false;h.removeCallbacksAndMessages(null)}
 override fun onAccessibilityEvent(e:AccessibilityEvent?){val p=e?.packageName?.toString()?:return;if(p==packageName)return;active=p;if(running && p!=target)stop()}
 override fun onInterrupt(){stop()};override fun onDestroy(){stop();overlay?.let{wm.removeView(it)};super.onDestroy()}
}