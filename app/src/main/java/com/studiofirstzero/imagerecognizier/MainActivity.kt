package com.studiofirstzero.imagerecognizier
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.studiohana.facerecognizer.DetectionChooser
import com.studiohana.facerecognizer.PermissionUtil
import com.studiohana.facerecognizer.UploadChooser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyize_view.*
import java.io.File
import java.io.IOException

class MainActivity : BaseActivity() {
    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser : UploadChooser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setValues()
        setupEvents()
    }

    override fun setupEvents() {
        uploadImg.setOnClickListener {
            uploadChooser = UploadChooser().apply {
                addNotifier(object : UploadChooser.NotifierInterface {
                    override fun cameraOnClick() {
                        Log.d("upload", "camera")
                        checkCameraPermission()
                    }

                    override fun galleryOnClick() {
                        Log.d("upload", "gallery")
                        checkGalleryPermission()
                    }

                })
            }
            uploadChooser!!.show(supportFragmentManager, "")
        }

    }

    override fun setValues() {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {

//           카메라 찍기 작업이 잘된 경우에만 동작할 부분을 오버라이드, 여기서는 사진을 저장해서 보여
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    this.createImageFile()
                )
                val bitmap = getBitmapFromUri(photoUri)
                uploadImage(bitmap)
            }

            GALLERY_PERMISSION_REQUEST -> {
                val photoUri = FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    this.createImageFile()
                )

                try {
                    val bitmap = getBitmapFromUri(data?.data!!)
                    val bitmapRotated = rotateBitmap(bitmap, 90f)
                    uploadImage(bitmapRotated)
                } catch (e: Exception) {
                    runOnUiThread {
                        val toast = Toast.makeText(
                            applicationContext,
                            "이미지만 선택해주세요",
                            Toast.LENGTH_SHORT
                        )
                        toast.setGravity(
                            Gravity.CENTER,
                            Gravity.CENTER_HORIZONTAL,
                            Gravity.CENTER_VERTICAL
                        )
                        toast.show()
                    }
                    Log.d("log", e.toString())
                }

            }

        }

    }

    private fun uploadImage(bitmapImage: Bitmap) {
        val chart = findViewById<BarChart>(R.id.barChart)
        val resultImage = findViewById<ImageView>(R.id.uploadedImg)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        Log.d("log", "Image is selected")
        try {
            val visionImageDetcetor = VisionImageDetcetor()
            uploadChooser?.dismiss()
            DetectionChooser().apply {
                addDetectionChooserNotifierInterface(object :
                    DetectionChooser.DetectionChooserNotifierInterface {
                    override fun detectLabel() {
                        val detectLabelResults =
                            visionImageDetcetor.detectLabels(bitmapImage).apply {
                                visionImageDetcetor.logResult(this)
                                runOnUiThread {
                                    progressBar.visibility = View.VISIBLE
                                    resultImage.setImageBitmap(bitmapImage)
                                }
                            }

                        Handler().postDelayed({
                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                chart.visibility = View.VISIBLE
//                                setChart(detectLabelResults)
                            }
                        }, 4000)
                    }

                    override fun detectLandmark() {
                        val detectLabelResults =
                            visionImageDetcetor.detectLandmarks(bitmapImage).apply {
                                visionImageDetcetor.logResult(this)
                                runOnUiThread {
                                    progressBar.visibility = View.VISIBLE
                                    resultImage.setImageBitmap(bitmapImage)
                                }
                            }

                        Handler().postDelayed({
                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                chart.visibility = View.VISIBLE
                                setChart(detectLabelResults)
                            }
                        }, 4000)
                    }

                })
            }.show(supportFragmentManager, "")
        } catch (e: Exception) {
            Log.d("log", e.toString())

        }
    }

    private fun getBitmapFromUri(imageUri: Uri) : Bitmap {
        lateinit var bitmap : Bitmap
        if (Build.VERSION.SDK_INT > 28) {
            val source: ImageDecoder.Source =
                ImageDecoder.createSource(applicationContext.contentResolver, imageUri)
            try {
                bitmap = ImageDecoder.decodeBitmap(source)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                bitmap =
                    MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, imageUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val matrix = Matrix()
        matrix.postRotate(orientation)

        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        bitmap.recycle()

        return resizedBitmap
    }

    private fun setChart(labelArrary: ArrayList<VisionDetectResult>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        try {
            for (i in 0..2) {
                val label = labelArrary.get(i)
                val entry = BarEntry(i.toFloat(), label.confidence * 100)
                entries.add(entry)
                labels.add("${label.name}")
            }
            val colors : ArrayList<Int> = ColorTemplate.COLORFUL_COLORS as ArrayList<Int>

            val barDataSet = BarDataSet(entries, "일치율")
            barDataSet.colors = colors

            val data = BarData(barDataSet)
            val xAxisFormatter = IndexAxisValueFormatter(labels)
            barChart.data = data
            barChart.animateY(500)
            barChart.apply {
                isDragEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                xAxis.valueFormatter = xAxisFormatter
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawLabels(true)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //    카메라 or 갤러리 선택시 실행할 로직을 결정하는 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            GALLERY_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(
                        requestCode,
                        GALLERY_PERMISSION_REQUEST,
                        grantResults
                    )
                )
                    openGallery()
            }

            CAMERA_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(
                        requestCode,
                        CAMERA_PERMISSION_REQUEST,
                        grantResults
                    )
                )
                    openCamera()
            }
        }
    }

//    권한 설정 및 인텐트 접근 관련
    private fun checkCameraPermission(){
        if (PermissionUtil().requestPermission(
                this,
                CAMERA_PERMISSION_REQUEST,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openCamera()
        }
    }

    private fun checkGalleryPermission(){
        if (PermissionUtil().requestPermission(
                this, GALLERY_PERMISSION_REQUEST,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openGallery()
        }
    }

    // 생성된 이미지 파일을 Uri를 통해 저장하는 함수
    private fun  openCamera() {
        val photoUri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            this.createImageFile()
        )
        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, CAMERA_PERMISSION_REQUEST
        )
    }

    private fun  openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            setType("image/*")
            setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            setAction(Intent.ACTION_GET_CONTENT)
        }

        startActivityForResult(
            Intent.createChooser(intent, "Select a photo"),
            GALLERY_PERMISSION_REQUEST
        )
    }

    private fun createImageFile() : File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.d("log", "image file path")
        return  File(dir, FILE_NAME)
    }


}