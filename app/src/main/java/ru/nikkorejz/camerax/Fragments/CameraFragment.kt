package ru.nikkorejz.camerax.Fragments

import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.common.util.concurrent.ListenableFuture
import ru.nikkorejz.camerax.R
import ru.nikkorejz.camerax.Utils.*
import ru.nikkorejz.camerax.databinding.FragmentCameraBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "dd.MM.yyyy HH:mm:ss"
    }

    private lateinit var binding: FragmentCameraBinding
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture
    private lateinit var executor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            sharedViewModel = ViewModelProvider(it)[SharedViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        binding.ButtonSave.setOnClickListener(::savePhoto)
        binding.ButtonTake.setOnClickListener(::takePhoto)
        return binding.root
    }

    private fun savePhoto(view: View) {
        // If the use case is null, exit out of the function.
        // This will be null If we tap the photo button before image capture is set up.
        // Without the return statement, the app would crash if it was null.
        if (!::imageCapture.isInitialized) {
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PATH")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun takePhoto(view: View) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, ImageViewFragment.newInstance(image))
                        .addToBackStack(null)
                        .commit()
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (::sharedViewModel.isInitialized) {
            sharedViewModel.isPermissionGranted.observe(viewLifecycleOwner) { value ->
                if (value) {
                    initCamera()
                }
            }
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun initCamera() {
        executor = Executors.newSingleThreadExecutor()
        val processCameraProvider: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(requireContext())
        processCameraProvider.addListener({
            val cameraProvider = processCameraProvider.get()
            cameraProvider.unbindAll()

            val cameraPreview = Preview.Builder().build()
            cameraPreview.setSurfaceProvider(binding.CameraPreview.surfaceProvider)

            imageCapture = with(ImageCapture.Builder()) {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                build()
            }

            imageAnalysis = with(ImageAnalysis.Builder()) {
                setResolutionSelector(
                    with(ResolutionSelector.Builder()) {
                        setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        build()
                    }
                )
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                build()
            }

//            CameraSelector.DEFAULT_BACK_CAMERA
//            OR
//            val cameraSelector = with(CameraSelector.Builder()) {
//                requireLensFacing(LENS_FACING_BACK)
//                build()
//            }

            var startTime = System.currentTimeMillis()
            imageAnalysis.clearAnalyzer()
            imageAnalysis.setAnalyzer(executor) {
                it.image?.let { image ->
                    Log.i(TAG, "Time between ticks: ${System.currentTimeMillis() - startTime}ms")
                    startTime = System.currentTimeMillis()
                    val bitmap = image.toAnalyzeBitmap().rotate(it.imageInfo.rotationDegrees.toFloat())
                    Log.i(TAG, "Conversion time: ${System.currentTimeMillis() - startTime}ms")

                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                    pixels.mapInPlace { pixel ->
                        val r = pixel shr 16 and 0xFF
                        val g = pixel shr 8 and 0xFF
                        val b = pixel shr 0 and 0xFF
                        val gray = (r + g + b) / 3
                        Color.argb(255, gray, gray, gray)
                    }

                    bitmap.setPixels(
                        pixels,
                        0,
                        bitmap.width,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height
                    )

                    activity?.let { parent ->
                        parent.runOnUiThread {
                            binding.CameraImageView.setImageBitmap(bitmap)
                        }
                    }
                }
                // Если не вызвать .close(), то библиотека будет считать, что изображение
                // еще обрабатывается и callback не будет вызван.
                it.close()
            }

            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis, cameraPreview, imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}