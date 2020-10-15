package com.example.demo0702_Classifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executors

//这是设置请求权限的code码
private const val REQUEST_CODE_PERMISSIONS = 1
//这是要获取的拍照权限
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE)
//读取相册的请求码
private const val IMAGE_REQUEST_CODE = 2


class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var lensFacing = CameraX.LensFacing.BACK //默认使用后摄像


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initEvent()

        // 判断是否有权限
        if (allPermissionsGranted()) {
            //Toast.makeText(this, "已获取拍照权限！", Toast.LENGTH_SHORT).show()
            view_finder.post{ startCamera()}
        } else {
            //请求权限
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    /**
     * 处理权限请求的结果 对话框中有被批准了？如果是，启动摄像机。否则请提示
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //Toast.makeText(this, "已获取拍照权限！", Toast.LENGTH_SHORT).show()
                view_finder.post{ startCamera()}
            } else {
                Toast.makeText(this, "没有权限，请授权！", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initEvent(){
        //取景器监听到布局变化时，重新计算布局
        view_finder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        //拍照
        btn_take.setOnClickListener {
            //创建文件
            val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
            imageCapture?.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        //保存失败
                        val msg = "保存失败: $message"
                        view_finder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onImageSaved(file: File) {
                        //保存成功
                       // val msg = "保存成功: ${file.absolutePath}"
                        view_finder.post {
                            //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            //这个广播的目的就是更新图库，发了这个广播进入相册就可以找到你保存的图片了！
                            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            val uri = Uri.fromFile(file)
                            intent.data = uri
                            sendBroadcast(intent)
                        }

                        // 跳转到照片预览窗体
                        val intent = Intent(this@MainActivity, PreviewActivity::class.java)
                        var imgUri = Uri.fromFile(File(file.absolutePath))
                        intent.putExtra("path",imgUri.toString())
                        startActivity(intent)
                    }
                })
        }

        //切换摄像头
        btn_switch.setOnClickListener {
            lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
                CameraX.LensFacing.BACK
            } else {
                CameraX.LensFacing.FRONT
            }
            startCamera()
        }

        // 从相册中选择
        btn_photo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type ="image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            IMAGE_REQUEST_CODE -> {
                val imgUri: Uri? = data?.data //获取系统返回的照片的Uri
                val intent = Intent(this@MainActivity, PreviewActivity::class.java)
                intent.putExtra("path", imgUri.toString())
                startActivity(intent)
            }
        }
    }

    /**
     * 检查权限是否被授权
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 初始化拍照
     */
    private fun startCamera() {
        //从生命周期中解除所有用例的绑定，并将它们从CameraX中移除。
        CameraX.unbindAll()
        //为取景器用例创建配置对象
        val previewConfig = PreviewConfig.Builder().apply {
            //根据此配置设置目标的比例
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            //设置取景器前后摄像头
            //setLensFacing(CameraX.LensFacing.BACK)
            setLensFacing(lensFacing)
        }.build()
        // 创建预览
        val preview = Preview(previewConfig!!)
        // 每次更新取景器时，重新计算布局
        preview?.setOnPreviewOutputUpdateListener {
            //要更新SurfaceTexture，我们必须删除它并重新添加它
            val parent = view_finder.parent as ViewGroup
            parent.removeView(view_finder)
            parent.addView(view_finder, 0)
            view_finder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }
        // 为图像捕获用例创建配置对象
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                //设置图像捕获模式
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
                //设置取景器前后摄像头
                //setLensFacing(CameraX.LensFacing.BACK)
                setLensFacing(lensFacing)
            }.build()
        // 构建图像捕获用例
        imageCapture = ImageCapture(imageCaptureConfig!!)
        // 将用例绑定到生命周期
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    /**
     * 根据设备方向的变化，更新视图
     */
    private fun updateTransform() {
        val matrix = Matrix()
        // 计算取景器的中心
        val centerX = view_finder.width / 2f
        val centerY = view_finder.height / 2f
        //校正预览输出以考虑显示旋转
        val rotationDegrees = when (view_finder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        //添加到TextureView
        view_finder.setTransform(matrix)
    }

}
