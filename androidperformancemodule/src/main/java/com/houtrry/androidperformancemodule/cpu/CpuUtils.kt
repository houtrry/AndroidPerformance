package com.houtrry.androidperformancemodule.cpu

import android.os.Process
import android.util.Log
import com.houtrry.androidperformancemodule.utils.readFile
import java.io.File

/**
 * @author: houtrry
 * @time: 2022/9/25
 * @desc:
 */
object CpuUtils {

    private const val TAG = "CpuUtils"

    fun readCpuInfo():String {
        val startTime = System.currentTimeMillis()
        val sb = StringBuilder()
        val pid = Process.myPid()
        val readStat = "/proc/$pid/stat".readFile()
        sb.append(readStat)
        Log.v(TAG, "/proc/$pid/stat: $readStat")
        val readSched = "/proc/$pid/sched".readFile()
        sb.append(readSched)
        Log.v(TAG, "/proc/$pid/sched: $readSched")
        val readLoadavg = "/proc/loadavg".readFile()
        sb.append(readLoadavg)
        Log.v(TAG, "/proc/loadavg: $readLoadavg")
//        val readTidStat = "/proc/$pid/task/${Process.myTid()}/stat".readFile()
//        sb.append(readTidStat)
//        Log.v(TAG, "/proc/$pid/task/${Process.myTid()}/stat: $readTidStat")

        File("/proc/$pid/task").list()?.forEach {
            val readTidStatt = "/proc/$it/stat".readFile()
            sb.append(readTidStatt)
            Log.v(TAG, "/proc/$it/stat: $readTidStatt")
        }
        Log.v(TAG, "readCpuInfo cost time: ${System.currentTimeMillis() - startTime}")
        return sb.toString()
    }
}