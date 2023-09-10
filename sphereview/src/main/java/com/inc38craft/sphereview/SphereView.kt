package com.inc38craft.sphereview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

open class SphereView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val renderer = SphereViewRenderer().apply {
        setOnSurfaceTextureCreated {
            onSurfaceTextureCreated(it)
        }
    }

    private var prevX = 0.0f
    private var prevY = 0.0f

    init {
        this.setEGLContextClientVersion(2)
        this.setRenderer(renderer)
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

    open fun onSurfaceTextureCreated(surfaceTexture: SurfaceTexture) {}
}