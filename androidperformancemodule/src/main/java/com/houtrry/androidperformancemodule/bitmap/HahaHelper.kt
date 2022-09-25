package com.houtrry.bitmapmonitormodule

import com.squareup.haha.perflib.ClassInstance

/**
 * @author: houtrry
 * @time: 2022/9/25
 * @desc:
 */
object HahaHelper {
    fun findValue(fields: List<ClassInstance.FieldValue>, value: String): Any? {
        return fields.find { it.field?.name == value }?.value
    }

}