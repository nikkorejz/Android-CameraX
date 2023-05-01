package ru.nikkorejz.camerax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageProxy
import androidx.fragment.app.Fragment
import ru.nikkorejz.camerax.Utils.rotate
import ru.nikkorejz.camerax.Utils.toBitmap
import ru.nikkorejz.camerax.databinding.FragmentImageBinding

class ImageViewFragment(private var imageProxy: ImageProxy) : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(image: ImageProxy) =
            ImageViewFragment(image)
    }

    private lateinit var binding: FragmentImageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageProxy.image?.let {
            binding.imageView.setImageBitmap(
                it.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            )
        }
    }
}