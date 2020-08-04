package com.studiofirstzero.imagerecognizier

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.studiohana.facerecognizer.DetectionChooser
import com.studiohana.facerecognizer.PermissionUtil
import com.studiohana.facerecognizer.UploadChooser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyize_view.*
import java.io.File

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
                addNotifier(object : UploadChooser.NotifierInterface{
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
        setChart()
    }

    private fun checkCameraPermission(){
        if (PermissionUtil().requestPermission(
                this, CAMERA_PERMISSION_REQUEST, android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openCamera()
        }
    }

    private fun checkGalleryPermission(){
        if (PermissionUtil().requestPermission(
                this, GALLERY_PERMISSION_REQUEST,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            openGallery()
        }
    }

    // 생성된 이미지 파일을 Uri를 통해 저장하는 함수
    private fun  openCamera() {
        val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, CAMERA_PERMISSION_REQUEST )
    }

    private fun  openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent,"Select a photo"), GALLERY_PERMISSION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
//           카메라 찍기 작업이 잘된 경우에만 동작할 부분을 오버라이드, 여기서는 사진을 저장해서 보여
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
                uploadImage(photoUri)
            }

            GALLERY_PERMISSION_REQUEST -> {
                data?.let {
                    it.data
                    val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
                    uploadImage(photoUri)
                }

            }

        }

    }

    // 원하는 이름으로 이미지 파일을 저장하는 함수
    private fun createCameraFile() : File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return  File(dir, FILE_NAME)
    }

    private fun uploadImage(imagerUri : Uri) {
        val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imagerUri)
        val visionImageDetcetor = VisionImageDetcetor()
        uploadChooser?.dismiss()
        DetectionChooser().apply {
            addDetectionChooserNotifierInterface(object : DetectionChooser.DetectionChooserNotifierInterface{
                override fun detectLabel() {
                    val detectLabelResults = visionImageDetcetor.detectLabels(bitmap).apply {
                        visionImageDetcetor.logResult(this)


                        runOnUiThread {
                            findViewById<ImageView>(R.id.uploadedImg).setImageBitmap(bitmap)
                        }

                    }

                }

                override fun detectLandmark() {
                    val detectLabelResults = visionImageDetcetor.detectLandmarks(bitmap).apply {
                        visionImageDetcetor.logResult(this)

                        runOnUiThread {
                            findViewById<ImageView>(R.id.uploadedImg).setImageBitmap(bitmap)
                        }

                    }

                }

            })
        }.show(supportFragmentManager, "")
    }

    private fun setChart() {
       val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(8f, 1f))
        entries.add(BarEntry(10f, 4f))

        val barDataSet = BarDataSet(entries, "Data Value")

        val labels = ArrayList<String>()
        labels.add("18-Jan")
        labels.add("19-Jan")

        val data = BarData(barDataSet)
        barChart.data = data // set the data and list of lables into chart

        //barDataSet.setColors(ColorTemplate.COLORFUL_COLORS)
        barDataSet.color = resources.getColor(R.color.colorAccent)

        barChart.animateY(5000)
    }

    //    카메라 or 갤러리 선택시 실행할 로직을 결정하는 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            GALLERY_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode, GALLERY_PERMISSION_REQUEST, grantResults))
                    openGallery()
            }

            CAMERA_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISSION_REQUEST, grantResults))
                    openCamera()
            }
        }
    }

}