package com.example.demo0702_Classifier

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var classifier: Classifier
    private var rgbFrameBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        // 接受传来的照片地址
        val path = intent.getStringExtra("path")
        // 如果地址不为空
        if (!path.isNullOrEmpty()) {
            // 显示照片
            img_preview.setImageURI(Uri.parse(path))
            // 加载模型
            initClassifier()
            // 点击识别
            initViews()
        }
    }

    private fun initClassifier() {
        classifier = Classifier(assets,
            "mobilenet_v1_1.0_224_quant.tflite",
            "labels_mobilenet_quant_v1_224.txt")
    }

    private fun initViews() {
        btn_recognition.setOnClickListener {
            //Toast.makeText(this,"准备识别", Toast.LENGTH_SHORT).show()
            val drawable: Drawable = img_preview.drawable
            rgbFrameBitmap = drawable.toBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)

            var result = rgbFrameBitmap?.let { it1 -> classifier.recognizeImage(it1) }

            if(result != null) {
                txt_result_title.text = "识别结果:" + result!!.title
                txt_result_confidence.text = " 置信度：" + result!!.confidence
            }else {
                txt_result_title.text ="没有识别到物体,"
                txt_result_confidence.text = " 请调整拍照背景和角度。"
            }

        }
    }
}
