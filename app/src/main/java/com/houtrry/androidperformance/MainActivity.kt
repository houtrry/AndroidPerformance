package com.houtrry.androidperformance

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.houtrry.androidperformancemodule.cpu.CpuUtils
import com.houtrry.androidperformancemodule.cpu.TimeMonitor
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_test_time_monitor).setOnClickListener {
            TimeMonitor.addMonitor("test", 5000)
            doCostTimeTask()
        }
    }

    private fun doCostTimeTask() {
        repeat(15){
            Thread({
                repeat(300) { count ->
                    Log.d("TimeMonitor", "$it-$count-print-(enable app-level tracing for a comma separated list of cmdlines)")
                    Thread.sleep(20)
                }
            },"Thread-$it-Test").start()
        }
    }

}