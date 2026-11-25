package com.seedfarm
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.widget.*
class MainActivity: Activity() {
 override fun onCreate(b: Bundle?) { super.onCreate(b)
 val p=getSharedPreferences("routine",0); val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32) }; setContentView(ScrollView(this).apply{addView(l)})
 l.addView(TextView(this).apply{text="Seed Auto Farm · Alpha\nUse only where automation is permitted. Account bans remain possible. Coordinates are screen percentages.";textSize=20f})
 fun field(label:String,key:String,def:String):EditText { l.addView(TextView(this).apply{text=label}); return EditText(this).apply{setText(p.getString(key,def));l.addView(this)} }
 val points=field("Routine: x,y,delay ms; one per line","points","50,60,2000")
 val minutes=field("Session minutes (0 = unlimited, 1–720)","minutes","0")
 l.addView(Button(this).apply{text="Save routine";setOnClickListener{try{parse(points.text.toString()); require(minutes.text.toString().toInt() in 0..720);p.edit().putString("points",points.text.toString()).putString("minutes",minutes.text.toString()).apply();Toast.makeText(this@MainActivity,"Saved",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this@MainActivity,"Invalid routine",Toast.LENGTH_LONG).show()}}})
 l.addView(Button(this).apply{text="Open accessibility settings";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
 }
}
data class Step(val x:Float,val y:Float,val delay:Long)
fun parse(s:String):List<Step> = s.lines().filter{it.isNotBlank()}.map { val a=it.split(',').map{v->v.trim()};require(a.size==3); val x=a[0].toFloat();val y=a[1].toFloat();val d=a[2].toLong();require(x in 0f..100f && y in 0f..100f && d in 500..60000);Step(x,y,d) }.also{require(it.size in 1..50)}