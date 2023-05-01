package ru.nikkorejz.camerax

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.common.util.concurrent.ListenableFuture
import ru.nikkorejz.camerax.Fragments.CameraFragment
import ru.nikkorejz.camerax.Fragments.VideoCameraFragment
import ru.nikkorejz.camerax.Utils.SharedViewModel
import ru.nikkorejz.camerax.databinding.ActivityMainBinding
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        private const val REQUEST_CAMERA = 101
        private val REQUEST_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private lateinit var viewModel: SharedViewModel

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, REQUEST_CAMERA)
        } else {
            viewModel.isPermissionGranted.value = true
        }

//        supportFragmentManager.beginTransaction().replace(R.id.MainActivityRoot, CameraFragment()).commit()
        supportFragmentManager.beginTransaction().replace(R.id.MainActivityRoot, VideoCameraFragment()).commit()
    }

    private fun allPermissionsGranted() = REQUEST_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CAMERA && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            initCamera()
//        }

        viewModel.isPermissionGranted.value = allPermissionsGranted()
    }
}