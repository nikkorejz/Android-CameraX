package ru.nikkorejz.camerax

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ru.nikkorejz.camerax.Fragments.VideoCameraFragment
import ru.nikkorejz.camerax.Utils.SharedViewModel
import ru.nikkorejz.camerax.databinding.ActivityMainBinding

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