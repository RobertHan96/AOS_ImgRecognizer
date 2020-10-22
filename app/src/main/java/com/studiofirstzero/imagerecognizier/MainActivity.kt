package com.studiofirstzero.imagerecognizier
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.studiohana.facerecognizer.DetectionChooser
import com.studiohana.facerecognizer.PermissionUtil
import com.studiohana.facerecognizer.UploadChooser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyize_view.*
import java.io.File
import java.io.IOException

class MainActivity : BaseActivity() {
    private var uploadChooser : UploadChooser? = null
    lateinit var permissionUtil : PermissionUtil

    override fun setupEvents() {
        uploadImg.setOnClickListener {
            permissionUtil = PermissionUtil()
            uploadChooser = UploadChooser().apply {
                addNotifier(object : UploadChooser.NotifierInterface {
                    override fun cameraOnClick() {
                        Log.d("upload", "camera")
                        if(permissionUtil.checkCameraPermission(mContext)) {
                            openCamera()
                        }
                    }

                    override fun galleryOnClick() {
                        Log.d("upload", "gallery")
                        if (permissionUtil.checkGalleryPermission(mContext)) {
                            openGallery()
                        }
                    }
            })
            }
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }

    override fun setValues() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setValues()
        setupEvents()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            permissionUtil.CAMERA_PERMISSION_REQUEST -> {
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", this.createImageFile())
                val resultImage = findViewById<ImageView>(R.id.uploadedImg)
                val bitmap = getBitmapFromUri(photoUri)
                setImageToView(mContext,photoUri)
            }

            permissionUtil.GALLERY_PERMISSION_REQUEST -> {
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", this.createImageFile())
                try {
                    setImageToView(mContext,data?.data!!)
                } catch (e: Exception) {
                    runOnUiThread {
                        val toast = Toast.makeText(applicationContext, R.string.image_type_error, Toast.LENGTH_SHORT)
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

    private fun setImageToView(context: Context, photoUri: Uri) {
    Glide.with(mContext).asBitmap().load(photoUri).into(object : CustomTarget<Bitmap>(){
            override fun onLoadCleared(placeholder: Drawable?) {
                Log.d("log", "image loading is finished.")
            }
            override fun onResourceReady(
                resource: Bitmap,
                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
            ) {
                val resultImage = findViewById<ImageView>(R.id.uploadedImg)
                uploadImage(resource)
                resultImage.setImageBitmap(resource)
            }
        })
    }

    private fun uploadImage(bitmapImage: Bitmap) {
        Log.d("log", "Image is uploading... detection start")
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
                                showDetectionResult(this)
                            }
                    }
                    override fun detectLandmark() {
                        val detectLabelResults =
                            visionImageDetcetor.detectLandmarks(bitmapImage).apply {
                                visionImageDetcetor.logResult(this)
                                showDetectionResult(this)
                            }
                    }
                })
            }.show(supportFragmentManager, "")
        } catch (e: Exception) {
            Log.d("log", e.toString())
        }
    }

    private fun showDetectionResult(vision : ArrayList<VisionDetectResult>) {
        runOnUiThread { progressBar.visibility = View.VISIBLE }
        Handler().postDelayed({
            if (vision.size < 4) {
                showDetectionFailErrorToast()
                runOnUiThread { progressBar.visibility = View.GONE }
            } else {
                detectionSuccessAnim(vision)
            }
        }, 4000)
    }

    private fun showDetectionFailErrorToast() {
        val toast = Toast.makeText(applicationContext,  R.string.detection_fail_error, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, Gravity.CENTER_HORIZONTAL, Gravity.CENTER_VERTICAL)
        toast.show()
    }

    private fun detectionSuccessAnim(vision : ArrayList<VisionDetectResult>) {
        val chart = findViewById<BarChart>(R.id.barChart)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val infromText = findViewById<TextView>(R.id.uploadedImgResult)
        runOnUiThread {
            progressBar.visibility = View.GONE
            infromText.visibility = View.INVISIBLE
            chart.visibility = View.VISIBLE
            setChart(vision)
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

    private fun setChart(labelArrary: ArrayList<VisionDetectResult>) {
        val chartDrawer = ChartDrawer()
        lateinit var chartData : BarData
        lateinit var indexLabels : ArrayList<String>

        if (chartDrawer.isValidDetectionResultArrary(labelArrary) == true ) {
            chartData = chartDrawer.getChartEntries(labelArrary)
            indexLabels = chartDrawer.getChartIndex(labelArrary)
            barChart.data = chartData
            barChart.animateY(500)
            barChart.apply {
                setScaleEnabled(false)
                setPinchZoom(false)
                isDragEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                xAxis.valueFormatter = IndexAxisValueFormatter(indexLabels)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawLabels(true)
                xAxis.labelCount = 3
            }
        } else {
            showDetectionFailErrorToast()
        }
    }

    //    카메라 or 갤러리 선택시 실행할 로직을 결정하는 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            permissionUtil.GALLERY_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode, permissionUtil.GALLERY_PERMISSION_REQUEST, grantResults) )
                    openGallery()
            }

            permissionUtil.CAMERA_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode, permissionUtil.CAMERA_PERMISSION_REQUEST, grantResults) )
                    openCamera()
            }
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
            }, permissionUtil.CAMERA_PERMISSION_REQUEST
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
            permissionUtil.GALLERY_PERMISSION_REQUEST
        )
    }

    private fun createImageFile() : File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.d("log", "image file path")
        return  File(dir, permissionUtil.FILE_NAME)
    }

}