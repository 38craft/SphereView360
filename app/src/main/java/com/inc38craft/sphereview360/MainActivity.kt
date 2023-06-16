package com.inc38craft.sphereview360

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Surface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.inc38craft.sphereview360.databinding.ActivityMainBinding
import com.wada811.databinding.dataBinding
import timber.log.Timber
import java.io.IOException

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val binding: ActivityMainBinding by dataBinding()
    private val mediaPlayer = MediaPlayer()

    private val fileSelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri->
                    openFile(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMenu()
        initEquirectanglarView()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
        binding.equirectangularView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.equirectangularView.onResume()
        if (!mediaPlayer.isPlaying && mediaPlayer.currentPosition > 1) {
            mediaPlayer.start()
        }
    }

    private fun initMenu() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.action_select_file -> {
                        selectFile()
                    }
                }
                return true
            }
        })
    }

    private fun initEquirectanglarView() {
        binding.equirectangularView.setOnSurfaceTextureCreated {
            mediaPlayer.setSurface(
                Surface(it)
            )
        }
    }

    private fun selectFile() {
        fileSelectLauncher.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/mp4"
            }
        )
    }

    private fun openFile(uri: Uri) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        mediaPlayer.setDataSource(applicationContext, uri)
        try {
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }
}