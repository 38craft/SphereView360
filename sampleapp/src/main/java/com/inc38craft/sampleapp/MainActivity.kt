package com.inc38craft.sampleapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.slider.Slider
import com.inc38craft.sampleapp.databinding.ActivityMainBinding
import com.wada811.databinding.dataBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val binding: ActivityMainBinding by dataBinding()

    private val fileSelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri->
                    kotlin.runCatching {
                        binding.playerView.playMedia(uri)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlayerView()
        initMenu()
        initSlider()
    }

    private fun initPlayerView() {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onPause(owner: LifecycleOwner) {
                    super.onPause(owner)
                    binding.playerView.onPause()
                }

                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    binding.playerView.onResume()
                }
            }
        )
    }

    private fun initSlider() {
        binding.slider.addOnChangeListener(
            Slider.OnChangeListener { _, value, _ ->
                val angle = 180 * (1 - value)
                binding.playerView.setFovAngle(angle)
            }
        )
        binding.slider.value = 0.2f
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

    private fun selectFile() {
        fileSelectLauncher.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/mp4"
            }
        )
    }
}