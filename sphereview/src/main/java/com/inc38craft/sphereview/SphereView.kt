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
            it.setOnFrameAvailableListener {
                requestRender()
            }
            onSurfaceTextureCreated(it)
        }
    }

    private var cameraPitchDegree = 0.0f
    private var prevX = 0.0f
    private var prevY = 0.0f

    init {
        this.setEGLContextClientVersion(2)
        this.setRenderer(renderer)
        this.renderMode = RENDERMODE_WHEN_DIRTY
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
                rotateCameraAngle(90.0f * deltaX / renderer.surfaceWidth, 90.0f * deltaY / renderer.surfaceHeight)
            }
        }
        return true
    }

    fun setFovAngle(degree: Float) {
        renderer.setFovAngle(degree)
    }

    fun setFlip(vertical: Boolean, horizontal: Boolean) {
        renderer.isHorizontalFlip = horizontal
        renderer.isVerticalFlip = vertical
    }

    fun resetCameraAngle() {
        cameraPitchDegree = 0.0f
        renderer.resetCameraAngle()
    }

    fun rotateCameraAngle(deltaYawDegree: Float, deltaPitchDegree: Float) {
        // Pitchは上下最大角を超えないようにする
        val trimmedDeltaPitchDegree =  (cameraPitchDegree + deltaPitchDegree).coerceIn(-MAX_CAMERA_PITCH_DEGREE, MAX_CAMERA_PITCH_DEGREE) - cameraPitchDegree
        cameraPitchDegree += trimmedDeltaPitchDegree
        renderer.rotateCameraAngle(deltaYawDegree, trimmedDeltaPitchDegree)
    }

    fun changeSurfaceTextureBufferSize(width:Int, height: Int) {
        renderer.setTextureBufferSize(width, height)
    }

    open fun onSurfaceTextureCreated(surfaceTexture: SurfaceTexture) {}

    companion object {
        const val MAX_CAMERA_PITCH_DEGREE = 70.0f
    }
}