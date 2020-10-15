package com.example.demo0702_Classifier

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        // 接受传来的照片地址
        val path = intent.getStringExtra("path")
        // 如果地址不为空
        if (!path.isNullOrEmpty()) {
            // 显示照片
            img_preview.setImageURI(Uri.parse(path))
        }

    }
}
