package com.houtrry.androidperformancemodule.cpu

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * @author: houtrry
 * @time: 2022/8/27
 * @desc: 如果指定时间内没移除监听任务， 则触发任务，获取当前进程的所有java线程堆栈信息
 */
object TimeMonitor {
    //CloseGuardHooker
    private const val TAG = "TimeMonitorHandle"
    private val mMainHandle by lazy { Handler(Looper.getMainLooper()) }
    private val mTaskMap by lazy { mutableMapOf<String, Pair<Runnable, Thread>>() }

    private var mTimeMonitorHandle: TimeMonitorHandle = { tag, thread, stackTraceElementArray ->
        val startTime = System.currentTimeMillis()
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        Log.d(TAG, "occur Time Monitor, tag: $tag, Tread is (${thread.id}, ${thread.name}, ${thread.state})")
        for (entry in stackTraceElementArray) {
            Log.d(TAG, "-----------------------------------------------------------")
            Log.d(TAG, "   ${entry.key.name} | id: ${entry.key.id}, state: ${entry.key.state}, isAlive: ${entry.key.isAlive}")
            for (stackTraceElement in entry.value) {
                Log.d(TAG, "    $stackTraceElement")
            }
        }
        Log.d(TAG, "-----------------------------------------------------------")
        Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
        Log.d(TAG, "TimeMonitorHandle cost time is ${System.currentTimeMillis() - startTime}")
        Log.d(TAG, "************************************************************")
    }

    fun setTimeMonitorHandle(t: TimeMonitorHandle) {
        mTimeMonitorHandle = t
    }

    fun addMonitor(tag: String, duration: Long) {
        val runnable = getTask(tag)
        mMainHandle.postDelayed(runnable, duration)
        mTaskMap[tag] = Pair(runnable, Thread.currentThread())
    }

    fun removeMonitor(tag: String) {
        mTaskMap.remove(tag)?.let {
            mMainHandle.removeCallbacks(it.first)
        }
    }


    private fun getTask(tag: String) = Runnable {
        mTaskMap.remove(tag)?.let {
            Log.d(TAG, "start do task")
            val startTime = System.currentTimeMillis()
            val allStackTraces = Thread.getAllStackTraces()
            Log.d(TAG, "getAllStackTraces cost time is ${System.currentTimeMillis() - startTime}")
            mTimeMonitorHandle.invoke(tag, it.second, allStackTraces)
        }
    }


}

typealias TimeMonitorHandle = (String, Thread, Map<Thread, Array<StackTraceElement>>) -> Unit