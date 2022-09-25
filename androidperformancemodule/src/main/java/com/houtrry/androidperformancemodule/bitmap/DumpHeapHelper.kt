package com.houtrry.bitmapmonitormodule

import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import android.util.SparseArray
import com.android.tools.perflib.captures.DataBuffer
import com.android.tools.perflib.captures.MemoryMappedFileBuffer
import com.squareup.haha.perflib.ArrayInstance
import com.squareup.haha.perflib.ClassInstance
import com.squareup.haha.perflib.Instance
import com.squareup.haha.perflib.Snapshot
import java.io.File
import java.util.*
import java.util.function.BiConsumer

/**
 * @author: houtrry
 * @time: 2022/9/25
 * @desc:
 */
object DumpHeapHelper {

    private const val TAG = "BitmapMonitorHelper"

    /**
     * 1. dump生成内存快照文件
     * 2. 解析内存快照
     * 3. 找到内存中的bitmap对象
     * 4. 根据Bitmap的mBuffer字段，判断是否是同一个图片（比较mBuffer的hashCode）
     *
     * 注意：8.0后，bitmap数据放在native中了，该方法也就失效了
     */
    fun analysisSameBitmap(context: Context) {
        Log.d(TAG, "test start")
        val startTime = System.currentTimeMillis()
        val heapDumpFile =
            File(context.filesDir.absoluteFile, "${System.currentTimeMillis()}.hprof")
        Debug.dumpHprofData(heapDumpFile.absolutePath)
        val buffer: DataBuffer = MemoryMappedFileBuffer(heapDumpFile)
        val snapshot = Snapshot.createSnapshot(buffer)
        //!!!这一步非常重要， 如果不调用，instance.nextInstanceToGcRoot会一直返回null
        snapshot.computeDominators()
        val heap = snapshot.getHeap("app")
        val bitmapClassObj = snapshot.findClass("android.graphics.Bitmap")
        Log.d(TAG, "${bitmapClassObj.instanceCount}, ${bitmapClassObj.instanceSize}")
        val heapInstances = bitmapClassObj.getHeapInstances(heap.id)

        val instanceMap = HashMap<Int, ArrayList<Instance>>()
        var instanceHashcode = 0
        heapInstances.forEach {
            if (it is ClassInstance) {
                //!!!此处尤其要注意，不一定有mBuffer，需要看Android版本
                val findValue = HahaHelper.findValue(it.values, "mBuffer")
                if (findValue is ArrayInstance) {
                    instanceHashcode = Arrays.hashCode(findValue.values)
                    val list = instanceMap[instanceHashcode] ?: ArrayList<Instance>()
                    list.add(it)
                    instanceMap[instanceHashcode] = list
                }
            }
        }
        for (value in instanceMap.values) {
            if (value.size <= 1) {
                Log.d(TAG, "only one image: $value")
                continue
            }
            Log.w(TAG, "--------start-------------")
            for (instance in value) {
                Log.d(TAG, "${instance.id}, ${instance.size}, ${instance.nativeSize} $instance")
//                for (stack in instance.stack.frames) {
//                    Log.d(TAG, "$stack")
//                }
                instance.printStack()
                Log.i(TAG, "------------------------")
            }
            Log.w(
                TAG,
                "--------end cost time is ${System.currentTimeMillis() - startTime}-------------"
            )
        }
    }

    private fun Instance.printStack() {
        var temp: Instance? = this
        while (temp != null) {
            Log.d(TAG, "    $temp")
            temp = temp.nextInstanceToGcRoot
        }
    }
}