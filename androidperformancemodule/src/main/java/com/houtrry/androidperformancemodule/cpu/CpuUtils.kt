package com.houtrry.androidperformancemodule.cpu

import android.os.Process
import android.util.Log
import com.houtrry.androidperformancemodule.utils.notNull
import com.houtrry.androidperformancemodule.utils.readFile
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author: houtrry
 * @time: 2022/9/25
 * @desc:
 */
object CpuUtils {

    private const val TAG = "CpuUtils"
    private val samplePeriod = 5_000L

    fun readCpuInfo1(): String {
        /**
         *
        // 获取 CPU 核心数
        cat /sys/devices/system/cpu/possible

        // 获取某个 CPU 的频率
        cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq
         */
        /**
        /proc/[pid]/stat             // 进程CPU使用情况
        /proc/[pid]/task/[tid]/stat  // 进程下面各个线程的CPU使用情况
        /proc/[pid]/sched            // 进程CPU调度相关
        /proc/loadavg                // 系统平均负载，uptime命令对应文件
         */
        val startTime = System.currentTimeMillis()
        val sb = StringBuilder()
        val pid = Process.myPid()
        val readStat = "/proc/stat".readFile()
        sb.appendLine("/proc/stat->$readStat")
        Log.v(TAG, "/proc/stat: $readStat")
        val readPidStat = "/proc/$pid/stat".readFile()
        sb.appendLine("/proc/$pid/stat->$readPidStat")
        Log.v(TAG, "/proc/$pid/stat: $readPidStat")
        val readSched = "/proc/$pid/sched".readFile()
        sb.appendLine("/proc/$pid/sched->$readSched")
        Log.v(TAG, "/proc/$pid/sched: $readSched")
        val readLoadavg = "/proc/loadavg".readFile()
        sb.appendLine("/proc/loadavg->$readLoadavg")
        Log.v(TAG, "/proc/loadavg: $readLoadavg")
//        val readTidStat = "/proc/$pid/task/${Process.myTid()}/stat".readFile()
//        sb.append(readTidStat)
//        Log.v(TAG, "/proc/$pid/task/${Process.myTid()}/stat: $readTidStat")

        File("/proc/$pid/task").list()?.forEach {
            val readTidStatt = "/proc/$pid/task/$it/stat".readFile()
            sb.appendLine("/proc/$pid/task/$it/stat->$readTidStatt")
            Log.v(TAG, "/proc/$pid/task/$it/stat: $readTidStatt")
        }
        Log.v(TAG, "readCpuInfo cost time: ${System.currentTimeMillis() - startTime}")
        return sb.toString()
    }

    /*

usage: CPU usage 5000ms(from 23:23:33.000 to 23:23:38.000):
System TOTAL: 2.1% user + 16% kernel + 9.2% iowait + 0.2% irq + 0.1% softirq + 72% idle
CPU Core: 8
Load Average: 8.74 / 7.74 / 7.36

Process:com.sample.app
  50% 23468/com.sample.app(S): 11% user + 38% kernel faults:4965

Threads:
  43% 23493/singleThread(R): 6.5% user + 36% kernel faults：3094
  3.2% 23485/RenderThread(S): 2.1% user + 1% kernel faults：329
  0.3% 23468/.sample.app(S): 0.3% user + 0% kernel faults：6
  0.3% 23479/HeapTaskDaemon(S): 0.3% user + 0% kernel faults：982
  ...
     */
    private val simpleDateFormat: SimpleDateFormat by lazy { SimpleDateFormat("HH:mm:ss.SSS") }
    suspend fun readCpuInfo():String {
        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()
        //读cpu信息
        val startCpuInfoMap = getCpuInfo(false, false)

        val pid = Process.myPid()
        //读当前进程的信息
        val startCurrentProgressCpuInfo = getProgressCpuInfo(pid)
        //当前的线程信息
        val startCurrentThreadCpuInfo = getThreadCpuInfo(pid)

        delay(samplePeriod)

        val endTime = System.currentTimeMillis()
        val endCpuInfoMap = getCpuInfo(true, true)
        val endCurrentProgressCpuInfo = getProgressCpuInfo(pid)
        //当前的线程信息
        val endCurrentThreadCpuInfo = getThreadCpuInfo(pid)


        val diffCpuInfoMap = diffCpuInfo(startCpuInfoMap, endCpuInfoMap)

        val diffProgressCpuInfo =
            diffProgressCpuInfo(startCurrentProgressCpuInfo, endCurrentProgressCpuInfo)

        val diffThreadCpuInfo =
            diffThreadCpuInfo(startCurrentThreadCpuInfo, endCurrentThreadCpuInfo)

        sb.appendLine(
            String.format(
                "usage: CPU usage %dms(from %s to %s):", samplePeriod,
                simpleDateFormat.format(Date(startTime)),
                simpleDateFormat.format(Date(endTime))
            )
        )

        diffCpuInfoMap.forEach {
            sb.appendLine(
                String.format(
                    "%s: %.1f user + %.1f kernel + %.1f iowait + %.1f irq + %.1f softirq + %.1f idle, %dHZ",
                    it.key,
                    it.value.user * 100.0 / it.value.total,
                    it.value.system * 100.0 / it.value.total,
                    it.value.iowait * 100.0 / it.value.total,
                    it.value.irq * 100.0 / it.value.total,
                    it.value.softirq * 100.0 / it.value.total,
                    it.value.idle * 100.0 / it.value.total,
                    it.value.freq
                )
            )
        }

        //读取cpu核心数
        val cpuCountInfo = "/sys/devices/system/cpu/possible".readFile()
        sb.append("CPU Core: ").appendLine(cpuCountInfo)
        Log.d(
            TAG,
            "cpuCountInfo: $cpuCountInfo, availableProcessors: ${
                Runtime.getRuntime()?.availableProcessors()
            }"
        )
        val readLoadavg = "/proc/loadavg".readFile()
        val parseLoadAvgInfo = parseLoadAvgInfo(readLoadavg)
        sb.append("Load Average: ")
            .append(parseLoadAvgInfo.loadAvg1)
            .append(" / ")
            .append(parseLoadAvgInfo.loadAvg5)
            .append(" / ")
            .append(parseLoadAvgInfo.loadAvg15)
            .appendLine()
            .appendLine()

        val cpuTimeOfProgress = diffCpuInfoMap["cpu${diffProgressCpuInfo.task_cpu}"]?.total
        cpuTimeOfProgress?.let {
            sb.append("Progress: ").append(diffProgressCpuInfo.name)
                .appendLine()
            printSingleProgressInfo(sb, diffProgressCpuInfo, diffCpuInfoMap)
            sb.appendLine()
        }

        sb.append("Threads: ").appendLine()

        diffThreadCpuInfo.forEach {
            printSingleProgressInfo(sb, it, diffCpuInfoMap)
        }

//        Log.e(TAG, sb.toString())
        return sb.toString()
    }

    private fun printSingleProgressInfo(
        sb: StringBuilder,
        progressCpuInfo: ProgressCpuInfo,
        diffCpuInfoMap: Map<String, CpuUtils.CpuInfo>
    ) {
        diffCpuInfoMap["cpu${progressCpuInfo.task_cpu}"]?.total?.let {
            Log.d(TAG, "$progressCpuInfo")
            sb.appendLine(
                String.format(
                    "  %.1f%% %d/%s(%s): %.1f user + %.1f kernel faults: %d",
                    progressCpuInfo.totalCpuTime * 100.0 / it,
                    progressCpuInfo.pid,
                    progressCpuInfo.name,
                    progressCpuInfo.task_state,
                    progressCpuInfo.totalCpuTime * 100.0 / it,
                    progressCpuInfo.totalCpuTime * 100.0 / it,
                    progressCpuInfo.maj_flt
                )
            )
        }
    }

    private fun diffThreadCpuInfo(
        startCurrentThreadCpuInfo: MutableMap<String, ProgressCpuInfo>,
        endCurrentThreadCpuInfo: MutableMap<String, ProgressCpuInfo>
    ): MutableList<ProgressCpuInfo> {
        val list = mutableListOf<ProgressCpuInfo>()
        endCurrentThreadCpuInfo.forEach {
            notNull(
                it.value,
                startCurrentThreadCpuInfo[it.key]
            ) { endThreadCpuInfo, startThreadCpuInfo ->
                list.add(diffProgressCpuInfo(startThreadCpuInfo, endThreadCpuInfo))
            }
        }
        list.sortByDescending { it.totalCpuTime }
        return list
    }

    private fun getThreadCpuInfo(pid: Int): MutableMap<String, ProgressCpuInfo> {
        val mutableMap = mutableMapOf<String, ProgressCpuInfo>()
        File("/proc/$pid/task").list()?.forEach {
            val readTidStatt = "/proc/$pid/task/$it/stat".readFile()
//            Log.v(TAG, "/proc/$pid/task/$it/stat: $readTidStatt")
            val decodeProgressCpuInfo = decodeProgressCpuInfo(readTidStatt)
//            Log.v(TAG, "/proc/$pid/task/$it/stat: $decodeProgressCpuInfo")
            mutableMap[decodeProgressCpuInfo.name] = decodeProgressCpuInfo
        }
        return mutableMap
    }

    private fun diffProgressCpuInfo(
        startCurrentProgressCpuInfo: ProgressCpuInfo,
        endCurentProgressCpuInfo: ProgressCpuInfo
    ): ProgressCpuInfo {
        endCurentProgressCpuInfo.totalCpuTime -= startCurrentProgressCpuInfo.totalCpuTime
        endCurentProgressCpuInfo.utime -= startCurrentProgressCpuInfo.utime
        endCurentProgressCpuInfo.stime -= startCurrentProgressCpuInfo.stime
        endCurentProgressCpuInfo.maj_flt -= startCurrentProgressCpuInfo.maj_flt
        return endCurentProgressCpuInfo
    }

    private fun getProgressCpuInfo(targetPid: Int): ProgressCpuInfo {
        val srcInfo = "/proc/$targetPid/stat".readFile()
        return decodeProgressCpuInfo(srcInfo)
    }

    private fun decodeProgressCpuInfo(srcInfo: String): ProgressCpuInfo {
        val nameStartIndex = srcInfo.indexOf("(")
        val nameEndIndex = srcInfo.indexOf(")")
        val name = srcInfo.subSequence(nameStartIndex + 1, nameEndIndex).toString()
        val stringTokenizer = StringTokenizer(srcInfo.substring(nameEndIndex + 1))
        val pid: Long = srcInfo.subSequence(0, nameStartIndex - 1).toString().toLong()
        val task_state: String = stringTokenizer.nextToken()
        val ppid: Long = stringTokenizer.nextToken().toLong() //父进程ID
        val pgid: Long = stringTokenizer.nextToken().toLong()//线程组号
        val sid: Long = stringTokenizer.nextToken().toLong()//该任务所在的会话组 ID
        val tty_nr: String = stringTokenizer.nextToken()//该任务的 tty 终端的设备号
        val tty_pgrp: Long = stringTokenizer.nextToken().toLong()//终端的进程组号
        val task_flags: Long = stringTokenizer.nextToken().toLong()//进程标志位
        val min_flt: Long = stringTokenizer.nextToken().toLong()//该任务不需要从硬盘拷数据而发生的缺页（次缺页）的次数
        val cmin_flt: Long =
            stringTokenizer.nextToken().toLong()//累计的该任务的所有的 waited-for 进程曾经发生的次缺页的次数目
        val maj_flt: Long = stringTokenizer.nextToken().toLong()//该任务需要从硬盘拷数据而发生的缺页（主缺页）的次数
        val cmaj_flt: Long =
            stringTokenizer.nextToken().toLong()//累计的该任务的所有的 waited-for 进程曾经发生的主缺页的次数目
        val utime: Long = stringTokenizer.nextToken().toLong()//该任务在用户态运行的时间，单位为 jiffies
        val stime: Long = stringTokenizer.nextToken().toLong()//该任务在核心态运行的时间，单位为 jiffies
        val cutime: Long =
            stringTokenizer.nextToken().toLong()//累计的该任务的所有的 waited-for 进程曾经在用户态运行的时间，单位为 jiffies
        val cstime: Long =
            stringTokenizer.nextToken().toLong()//累计的该任务的所有的 waited-for 进程曾经在核心态运行的时间，单位为 jiffies
        val priority: Int = stringTokenizer.nextToken().toInt()//任务的动态优先级
        val nice: Int = stringTokenizer.nextToken().toInt()//任务的静态优先级
        val num_threads: Int = stringTokenizer.nextToken().toInt()//该任务所在的线程组里线程的个数
        val it_real_value: Long =
            stringTokenizer.nextToken().toLong()//由于计时间隔导致的下一个 SIGALRM 发送进程的时延，以 jiffy 为单位
        val start_time: Long = stringTokenizer.nextToken().toLong()//该任务启动的时间，以 jiffy 为单位
        val vsize: Long = stringTokenizer.nextToken().toLong()//该任务的虚拟地址空间大小
        val rss: Long = stringTokenizer.nextToken().toLong()//该任务当前驻留物理地址空间的大小
        val rlim: Long = stringTokenizer.nextToken().toLong()//该任务能驻留物理地址空间的最大值
        val start_code: Long = stringTokenizer.nextToken().toLong()//该任务在虚拟地址空间的代码段的起始地址
        val end_code: Long = stringTokenizer.nextToken().toLong()//该任务在虚拟地址空间的代码段的结束地址
        val start_stack: Long = stringTokenizer.nextToken().toLong()//该任务在虚拟地址空间的栈的结束地址
        val kstkesp: Long = stringTokenizer.nextToken()
            .toLong()//esp(32 位堆栈指针) 的当前值 = stringTokenizer.nextToken().toLong() 与在进程的内核堆栈页得到的一致
        val kstkeip: Long = stringTokenizer.nextToken()
            .toLong()//指向将要执行的指令的指针 = stringTokenizer.nextToken().toLong() EIP(32 位指令指针)的当前值
        val pendingsig: Long = stringTokenizer.nextToken().toLong()//待处理信号的位图，记录发送给进程的普通信号
        val block_sig: Long = stringTokenizer.nextToken().toLong()//阻塞信号的位图
        val sigign: Long = stringTokenizer.nextToken().toLong()//忽略的信号的位图
        val sigcatch: Long = stringTokenizer.nextToken().toLong()//被俘获的信号的位图
        val wchan: Long = stringTokenizer.nextToken().toLong()//如果该进程是睡眠状态，该值给出调度的调用点
        val nswap: Long = stringTokenizer.nextToken().toLong()//被 swapped 的页数，当前没用
        val cnswap: Long = stringTokenizer.nextToken().toLong()//所有子进程被 swapped 的页数的和，当前没用
        val exit_signal: Long = stringTokenizer.nextToken().toLong()//该进程结束时，向父进程所发送的信号
        val task_cpu: Long = stringTokenizer.nextToken().toLong()//运行在哪个cpu上
        val task_rt_priority: Long = stringTokenizer.nextToken().toLong()//实时进程的相对优先级别
        val task_policy: Int =
            stringTokenizer.nextToken().toInt()//进程的调度策略，0=非实时进程，1=FIFO实时进程；2=RR实时进程
        val progressCpuTime: Long = utime + stime + cutime + cstime//进程占用的cpu时间

        return ProgressCpuInfo(
            pid,
            name,
            task_state,
            progressCpuTime,
            ppid,
            pgid,
            sid,
            tty_nr,
            tty_pgrp,
            task_flags,
            min_flt,
            cmin_flt,
            maj_flt,
            cmaj_flt,
            utime,
            stime,
            cutime,
            cstime,
            priority,
            nice,
            num_threads,
            it_real_value,
            start_time,
            vsize,
            rss,
            rlim,
            start_code,
            end_code,
            start_stack,
            kstkesp,
            kstkeip,
            pendingsig,
            block_sig,
            sigign,
            sigcatch,
            wchan,
            nswap,
            cnswap,
            exit_signal,
            task_cpu,
            task_rt_priority,
            task_policy
        )
    }

    private fun diffCpuInfo(
        startCpuInfoMap: Map<String, CpuUtils.CpuInfo>,
        endCpuInfoMap: Map<String, CpuUtils.CpuInfo>
    ): Map<String, CpuUtils.CpuInfo> {
        endCpuInfoMap.forEach {
            notNull(startCpuInfoMap[it.key], it.value) { startCpuInfo, endCpuInfo ->
                endCpuInfo.total -= startCpuInfo.total
                endCpuInfo.user -= startCpuInfo.user
                endCpuInfo.nice -= startCpuInfo.nice
                endCpuInfo.system -= startCpuInfo.system
                endCpuInfo.idle -= startCpuInfo.idle
                endCpuInfo.iowait -= startCpuInfo.iowait
                endCpuInfo.irq -= startCpuInfo.irq
                endCpuInfo.softirq -= startCpuInfo.softirq
                endCpuInfo.stealstolen -= startCpuInfo.stealstolen
                endCpuInfo.guest -= startCpuInfo.guest
            }
        }
        return endCpuInfoMap
    }

//    private fun parseCpuStatInfo(cpuFilePath: String): MutableMap<String, CpuInfo> {
////        readFileOneLineByOneLine(cpuFilePath)
//        File(cpuFilePath).readLines()
//            .filter { it.startsWith("cpu") }
//            .map {  }
//        return mutableMapOf()
//    }

    private fun parseCpuStatInfo(cpuInfo: String): CpuInfo {
        val stringTokenizer = StringTokenizer(cpuInfo)
        val name = stringTokenizer.nextToken()
        val user: Long = stringTokenizer.nextToken().toLong()
        val nice: Long = stringTokenizer.nextToken().toLong()
        val system: Long = stringTokenizer.nextToken().toLong()
        val idle: Long = stringTokenizer.nextToken().toLong()
        val iowait: Long = stringTokenizer.nextToken().toLong()
        val irq: Long = stringTokenizer.nextToken().toLong()
        val softirq: Long = stringTokenizer.nextToken().toLong()
        val stealstolen: Long = stringTokenizer.nextToken().toLong()
        val guest: Long = stringTokenizer.nextToken().toLong()
        val total: Long = user + nice + system + idle + iowait + irq + softirq + stealstolen + guest
        return CpuInfo(
            name,
            -1,
            total,
            user,
            nice,
            system,
            idle,
            iowait,
            irq,
            softirq,
            stealstolen,
            guest,
            0,
            0.0
        )
    }

    private fun parseLoadAvgInfo(loadavg: String): LoadAvgInfo {
        val stringTokenizer = StringTokenizer(loadavg)
        //1分钟的平均负载
        val loadAvg1 = stringTokenizer.nextToken().toDouble()
        //5分钟的平均负载
        val loadAvg5 = stringTokenizer.nextToken().toDouble()
        //15分钟的平均负载
        val loadAvg15 = stringTokenizer.nextToken().toDouble()
        return LoadAvgInfo(loadAvg1, loadAvg5, loadAvg15)
    }

    private fun getCpuInfo(needFreq: Boolean, needTemp: Boolean): Map<String, CpuInfo> {
        return File("/proc/stat").readLines()
            .filter { it.startsWith("cpu") }
            .map {
                val cpuInfo = parseCpuStatInfo(it)
                if (needFreq) {
                    val freqStr = "/sys/devices/system/cpu/${cpuInfo.name}/cpufreq/cpuinfo_cur_freq".readFile()
                    cpuInfo.freq = if (freqStr.isEmpty()) 0 else freqStr.toLong()
                }
                cpuInfo.name to cpuInfo
            }.toMap()
    }

    private data class LoadAvgInfo(
        val loadAvg1: Double,//1分钟的平均负载
        val loadAvg5: Double,//5分钟的平均负载
        val loadAvg15: Double//15分钟的平均负载
    )

    private data class CpuInfo(
        val name: String, //cpu核名称
        var cpuid: Long,//核id
        var total: Long, //总节拍数 total = user + nice + system + idle + iowait + irq + softirq + stealstolen + guest
        var user: Long,//
        var nice: Long,
        var system: Long,
        var idle: Long,
        var iowait: Long,
        var irq: Long,
        var softirq: Long,
        var stealstolen: Long,
        var guest: Long,
        var freq: Long,//频率
        var temp: Double//温度
    )

    private data class ProgressCpuInfo(
        val pid: Long,//进程(包括轻量级进程，即线程)号
        val name: String,//进程名
        val task_state: String,//任务的状态
        var totalCpuTime: Long,//进程占用的cpu时间
        val ppid: Long, //父进程ID
        val pgid: Long,//线程组号
        val sid: Long,//该任务所在的会话组 ID
        val tty_nr: String,//该任务的 tty 终端的设备号
        val tty_pgrp: Long,//终端的进程组号
        val task_flags: Long,//进程标志位
        val min_flt: Long,//该任务不需要从硬盘拷数据而发生的缺页（次缺页）的次数
        val cmin_flt: Long,//累计的该任务的所有的 waited-for 进程曾经发生的次缺页的次数目
        var maj_flt: Long,//该任务需要从硬盘拷数据而发生的缺页（主缺页）的次数
        val cmaj_flt: Long,//累计的该任务的所有的 waited-for 进程曾经发生的主缺页的次数目
        var utime: Long,//该任务在用户态运行的时间，单位为 jiffies
        var stime: Long,//该任务在核心态运行的时间，单位为 jiffies
        val cutime: Long,//累计的该任务的所有的 waited-for 进程曾经在用户态运行的时间，单位为 jiffies
        val cstime: Long,//累计的该任务的所有的 waited-for 进程曾经在核心态运行的时间，单位为 jiffies
        val priority: Int,//任务的动态优先级
        val nice: Int,//任务的静态优先级
        val num_threads: Int,//该任务所在的线程组里线程的个数
        val it_real_value: Long,//由于计时间隔导致的下一个 SIGALRM 发送进程的时延，以 jiffy 为单位
        val start_time: Long,//该任务启动的时间，以 jiffy 为单位
        val vsize: Long,//该任务的虚拟地址空间大小
        val rss: Long,//该任务当前驻留物理地址空间的大小
        val rlim: Long,//该任务能驻留物理地址空间的最大值
        val start_code: Long,//该任务在虚拟地址空间的代码段的起始地址
        val end_code: Long,//该任务在虚拟地址空间的代码段的结束地址
        val start_stack: Long,//该任务在虚拟地址空间的栈的结束地址
        val kstkesp: Long,//esp(32 位堆栈指针) 的当前值, 与在进程的内核堆栈页得到的一致
        val kstkeip: Long,//指向将要执行的指令的指针, EIP(32 位指令指针)的当前值
        val pendingsig: Long,//待处理信号的位图，记录发送给进程的普通信号
        val block_sig: Long,//阻塞信号的位图
        val sigign: Long,//忽略的信号的位图
        val sigcatch: Long,//被俘获的信号的位图
        val wchan: Long,//如果该进程是睡眠状态，该值给出调度的调用点
        val nswap: Long,//被 swapped 的页数，当前没用
        val cnswap: Long,//所有子进程被 swapped 的页数的和，当前没用
        val exit_signal: Long,//该进程结束时，向父进程所发送的信号
        val task_cpu: Long,//运行在哪个cpu上
        val task_rt_priority: Long,//实时进程的相对优先级别
        val task_policy: Int,//进程的调度策略，0=非实时进程，1=FIFO实时进程；2=RR实时进程
    )
}