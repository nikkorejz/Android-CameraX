package ru.nikkorejz.camerax

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import ru.nikkorejz.camerax.databinding.ActivityOpenCvbaseBinding


class OpenCVBaseActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        const val TAG = "OpenCVBaseActivity"
        private const val REQUEST_CAMERA = 202
        private val REQUEST_PERMISSIONS =
            arrayOf(
                android.Manifest.permission.CAMERA,
            )
    }

    private lateinit var cameraBridge: CameraBridgeViewBase
    private lateinit var binding: ActivityOpenCvbaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        binding = ActivityOpenCvbaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUEST_PERMISSIONS,
                REQUEST_CAMERA
            )
        } else {
            initCamera()
        }
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
        if (allPermissionsGranted()) {
            initCamera()
        }
    }

    private fun initCamera() {
        cameraBridge = binding.view
        cameraBridge.setCameraPermissionGranted()
        cameraBridge.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY)
        cameraBridge.visibility = SurfaceView.VISIBLE
        cameraBridge.setCvCameraViewListener(this)
        cameraBridge.enableFpsMeter()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        OpenCVLoader.initDebug();
        cameraBridge.enableView();
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()
        if (::cameraBridge.isInitialized)
            cameraBridge.disableView();
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
        if (::cameraBridge.isInitialized)
            cameraBridge.disableView();
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.i(TAG, "onCameraViewStarted")
    }

    override fun onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        Log.i(TAG, "onCameraFrame")
        if (inputFrame == null) {
            throw java.lang.IllegalStateException("Bad frame")
        }
        val rgba = inputFrame.rgba()

//        Rect.
        val rect = Rect(10, 10, 250, 150);
        Imgproc.rectangle(
            rgba,
            Point(rect.x.toDouble(), rect.y.toDouble()),
            Point(rect.x.toDouble() + rect.width, rect.y.toDouble() + rect.height),
            Scalar(255.0, 0.0, 0.0, 255.0),
            3
        )

//        Color scheme.
//        val newMat = Mat()
//        Grayscale
//        Imgproc.cvtColor(rgba, newMat, Imgproc.COLOR_RGBA2GRAY)
//        Imgproc.cvtColor(rgba, newMat, Imgproc.COLOR_RGBA2BGR)

//        Text.
        Imgproc.putText(
            rgba,
            "Sample text!",
            Point(0.0, 300.0),
            Imgproc.FONT_HERSHEY_SIMPLEX,
            2.6,
            Scalar(255.0, 255.0, 0.0), // BGR
            2
        )

//        val binMat = Mat()
////        Imgproc.adaptiveThreshold(newMat, binMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 40);
////                Imgproc.adaptiveThreshold(newMat, binMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 40);
//        Imgproc.threshold(newMat, binMat, 127.0, 255.0, Imgproc.THRESH_BINARY)
//        return binMat
        return rgba
    }
}