package com.inc38craft.sphereview

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import java.io.IOException

class MediaPlayer360View(
    context: Context,
    attrs: AttributeSet? = null
) : SphereView(context, attrs) {

    private val mediaPlayer = MediaPlayer()

    @Throws(IOException::class)
    fun playMedia(uri: Uri) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        mediaPlayer.setDataSource(context, uri)
        mediaPlayer.isLooping = true
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mediaPlayer.isPlaying && mediaPlayer.currentPosition > 1) {
            mediaPlayer.start()
        }
    }

    override fun onSurfaceTextureCreated(surfaceTexture: SurfaceTexture) {
        val surface = Surface(surfaceTexture)
        mediaPlayer.setSurface(surface)
        surface.release()
    }
}