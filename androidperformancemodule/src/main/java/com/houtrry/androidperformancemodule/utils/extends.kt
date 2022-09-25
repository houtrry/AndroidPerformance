package com.houtrry.androidperformancemodule.utils

import android.util.Log
import java.io.*

/**
 * @author: houtrry
 * @time: 2022/9/25
 * @desc:
 */

/**
 * 读文件字符串
 */
fun String.readFile(): String {
    val file = File(this)
    if (!file.exists()) {
        Log.d("readFile", "${file.absoluteFile} not exists")
        return ""
    }
    var fis: FileInputStream? = null
    var isr: InputStreamReader? = null
    var bfr: BufferedReader? = null
    val sb = StringBuffer()
    try {
        fis = FileInputStream(file)
        isr = InputStreamReader(fis)
        bfr = BufferedReader(isr)
        var text:String? = bfr.readLine()
        while (text != null) {
            sb.append(text)
            text = bfr.readLine()
        }
    } catch(e: IOException) {
        e.printStackTrace()
        Log.e("readFile", "error: ${e.message}")
    } finally {
        bfr?.close()
        isr?.close()
        fis?.close()
    }
    return sb.toString()
}

/**
 * 字符串写文件
 */
fun String.writeFile(file: File): Boolean {
    if (!file.exists()) {
        file.createNewFile()
    }
    var fos: FileOutputStream? = null
    var osw: OutputStreamWriter? = null
    var bfw: BufferedWriter? = null
    val sb = StringBuffer()
    try {
        fos = FileOutputStream(file)
        osw = OutputStreamWriter(fos)
        bfw = BufferedWriter(osw)
        bfw.write(this)
        bfw.flush()
        return true
    } catch(e: IOException) {
        e.printStackTrace()
    } finally {
        bfw?.close()
        osw?.close()
        fos?.close()
    }
    return false
}