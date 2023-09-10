package com.inc38craft.sphereview.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

fun newFloatBuffer(array: FloatArray): FloatBuffer {
    val buf = ByteBuffer.allocateDirect(array.size * java.lang.Float.SIZE / 8)
    buf.order(ByteOrder.nativeOrder())
    return buf.asFloatBuffer().apply {
        put(array)
        position(0)
    }
}

fun newIntBuffer(array: IntArray): IntBuffer {
    val buf = ByteBuffer.allocateDirect(array.size * 4)
    buf.order(ByteOrder.nativeOrder())
    return buf.asIntBuffer().apply {
        put(array)
        position(0)
    }
}