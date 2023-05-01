package ru.nikkorejz.camerax.Fragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import ru.nikkorejz.camerax.R
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.common.util.concurrent.ListenableFuture
import ru.nikkorejz.camerax.Utils.*
import ru.nikkorejz.camerax.databinding.FragmentCameraBinding
import ru.nikkorejz.camerax.databinding.FragmentVideoCameraBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoCameraFragment : Fragment() {

    companion object {
        private const val TAG = "VideoCameraFragment"
        private const val FILENAME_FORMAT = "dd.MM.yyyy HH:mm:ss"
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var binding: FragmentVideoCameraBinding
    private lateinit var sharedViewModel: SharedViewModel
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
        binding = FragmentVideoCameraBinding.inflate(inflater, container, false)
        binding.ButtonRecord.setOnClickListener(::recordVideo)
        return binding.root
    }

    private fun recordVideo(view: View) {
        val videoCapture = videoCapture ?: return
        binding.ButtonRecord.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(requireContext(), mediaStoreOutputOptions)
            .apply {
                // Enable Audio for recording
                if (
                    PermissionChecker.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.ButtonRecord.apply {
                            text = "Stop recording"
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: ${recordEvent.outputResults.outputUri}"
//                            parentFragmentManager.beginTransaction()
//                                .replace(R.id.container, VideoViewFragment.newInstance(recordEvent.outputResults.outputUri))
//                                .addToBackStack(null)
//                                .commit()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                        }
                        binding.ButtonRecord.apply {
                            text = "Record video"
                            isEnabled = true
                        }
                    }
                }
            }
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

            val cameraPreview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.CameraPreview.surfaceProvider)
            }

            // Video
            val recorder = Recorder.Builder()
                // Если появляются артефакты при записи, то нужно настроить соотношение сторон
                .setAspectRatio(RATIO_4_3)
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HIGHEST,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview, videoCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}