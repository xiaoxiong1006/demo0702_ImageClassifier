package com.example.demo0702_Classifier

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(assetManager: AssetManager, modelPath: String, labelPath: String){
    private var interpreter: Interpreter
    private var labelList: List<String>

    private var imageSizeX:Int
    private var imageSizeY:Int
    private var pixelSize:Int

    init {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(5)
        tfliteOptions.setUseNNAPI(true)
        interpreter = Interpreter(loadModelFile(assetManager, modelPath),tfliteOptions)
        labelList = loadLabelList(assetManager, labelPath)
        Log.i(this::class.simpleName,"\n=====模型加载成功=====")

        val imageTensorIndex = 0
        val imageShape: IntArray =
            interpreter.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}

        imageSizeY = imageShape[1]
        imageSizeX = imageShape[2]
        pixelSize = imageShape[3]
    }

    fun recognizeImage(bitmap: Bitmap){
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSizeX ,imageSizeY, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        Log.i(this::class.simpleName,"\n=====图片转换成功=====")

        val result = Array(1) { ByteArray(labelList.size) }
        interpreter.run(byteBuffer, result)
        Log.i(this::class.simpleName,"\n=====结果是否为空:" + result.isEmpty())
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        return assetManager.open(labelPath).bufferedReader().useLines { it.toList() }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap):ByteBuffer{
        val imgData = ByteBuffer.allocateDirect(imageSizeX * imageSizeY * pixelSize)
        imgData.order(ByteOrder.nativeOrder())
        imgData.rewind()

        val intValues = IntArray(imageSizeX *imageSizeY)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val value = intValues[pixel++]

                imgData.put((value.shr(16) and 0xFF).toByte())
                imgData.put((value.shr(8) and 0xFF).toByte())
                imgData.put((value and 0xFF).toByte())
            }
        }
        return imgData
    }
}