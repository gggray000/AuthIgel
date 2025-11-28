package com.ray.authigel.util

import androidx.camera.core.ImageProxy

object YuvToNv21Converter {

    fun yuvToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y
        yBuffer.get(nv21, 0, ySize)

        // V then U (NV21 format)
        val chromaStart = ySize
        val vBytes = ByteArray(vSize)
        vBuffer.get(vBytes)
        System.arraycopy(vBytes, 0, nv21, chromaStart, vSize)

        val uBytes = ByteArray(uSize)
        uBuffer.get(uBytes)
        System.arraycopy(uBytes, 0, nv21, chromaStart + vSize, uSize)

        return nv21
    }
}