package com.inc38craft.sphereview360.equirectanglarview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class EquirectangularView(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val renderer = EquirectangularRenderer().apply {
        setOnSurfaceTextureCreated {
            onSurfaceTextureCreated(it)
        }
    }

    private var surfaceTextureCreatedCb: ((surfaceTexture: SurfaceTexture) -> Unit)? = null
    private var prevX = 0.0f
    private var prevY = 0.0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }

    fun setOnSurfaceTextureCreated(callback: ((surfaceTexture: SurfaceTexture) -> Unit)?) {
        surfaceTextureCreatedCb = callback
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = prevX - event.x
                val deltaY = prevY - event.y
                prevX = event.x
                prevY = event.y
                renderer.rotateCameraAngle(90.0f * deltaX / renderer.surfaceWidth, 90.0f * deltaY / renderer.surfaceHeight)
            }
        }
        return true
    }

    private fun onSurfaceTextureCreated(surfaceTexture: SurfaceTexture) {
        surfaceTextureCreatedCb?.invoke(surfaceTexture)
    }
}