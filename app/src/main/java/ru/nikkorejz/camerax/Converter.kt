package ru.nikkorejz.camerax

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


object Converter {

    fun toBitmap(image: Image): Bitmap? {

//        val buffer: ByteBuffer = image.planes[0].buffer
//        val bytes = ByteArray(buffer.capacity())
//        buffer.get(bytes)
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

}