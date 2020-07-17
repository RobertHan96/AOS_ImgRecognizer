package com.studiohana.facerecognizer

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import kotlinx.android.synthetic.main.main_analyize_view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class LabelDetectionTask(
    private val packageName : String,
    private val packageManager : PackageManager,
    private  val activity: MainActivity

) {
    private val CLOUD_VISION_API_KEY = "AIzaSyC9hjasqA5RczIJHV2mMOYx7ipQRa5DQ54"
    private val ANDROID_PACKAGE_HEADER = "X-Android-Package"
    private val ANDROID_CERT_HEADER = "X-Android-Cert"
    private val MAX_LABEL_RESULTS = 10
    private var labelDetctionNotifierInterface : LabelDetctionNotifierInterface? = null
    private var requestType : String? = null

    interface LabelDetctionNotifierInterface {
        fun notifiyResult(result: String)
    }

    fun requestCloudVisionApi(
        img : Bitmap,
        labelDetctionNotifierInterface: LabelDetctionNotifierInterface,
        requestType : String
    ) {
        this.labelDetctionNotifierInterface = labelDetctionNotifierInterface
        this.requestType = requestType
        val visionTask = ImageRequestTask(prepareImageRequest(img))
        visionTask.execute()
    }


    inner class ImageRequestTask constructor(
        val request : Vision.Images.Annotate
    ) : AsyncTask<Any, Void, String>() {
        private val weakReference : WeakReference<MainActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun doInBackground(vararg params: Any?): String {
            try {
                val reponse = request.execute()
                return findProperResponseType(reponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "분석 실패"
        }

        override fun onPostExecute(result: String?) {
            val activity = weakReference.get()
            Log.d("test", "결과${result}")
            result?.let {
                labelDetctionNotifierInterface?.notifiyResult(it)
            }
        }

    }

    private fun prepareImageRequest(bitmap: Bitmap): Vision.Images.Annotate {
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val requestInitializer = object : VisionRequestInitializer(CLOUD_VISION_API_KEY){
            override fun initializeVisionRequest(request: VisionRequest<*>?) {
                super.initializeVisionRequest(request)
                val packageName = packageName
                request?.requestHeaders?.set(ANDROID_PACKAGE_HEADER, packageName)

                val sig = PackageManagerUtil().getSignature(packageManager, packageName)
                request?.requestHeaders?.set(ANDROID_CERT_HEADER, sig)
            }
        }
        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        val vision = builder.build()
        builder.setVisionRequestInitializer(requestInitializer)
        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        batchAnnotateImagesRequest.requests = object : ArrayList<AnnotateImageRequest>() {
            init {
                val annotateImageRequest = AnnotateImageRequest()
                val base64EncodedImage = Image()
                val byteArraryOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,byteArraryOutputStream)
                val imageBytes = byteArraryOutputStream.toByteArray()
                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image = base64EncodedImage
                annotateImageRequest.features = object : ArrayList<Feature>() {
                    init {
                        val labelDescription = Feature()
                        when(requestType) {
                            activity.LABEL_DETECTION_REQUEST-> {labelDescription.type = "LABEL_DETECTION"}
                            activity.LANDMARK_DETECTION_REQUEST-> {labelDescription.type = "LANDMARK_DETECTION"}
                        }
                        labelDescription.maxResults = MAX_LABEL_RESULTS
                        add(labelDescription)
                    }
                }
                add(annotateImageRequest )
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
        annotateRequest.setDisableGZipContent(true)
        return annotateRequest
    }

    private fun findProperResponseType(response: BatchAnnotateImagesResponse):String {
        when (requestType){
            activity.LABEL_DETECTION_REQUEST -> { return convertResponseToString(response.responses[0].labelAnnotations) }
            activity.LANDMARK_DETECTION_REQUEST -> { return convertResponseToString(response.responses[0].landmarkAnnotations) }
        }
        return "분석 실패"
    }

    private fun convertResponseToString(labels: List<EntityAnnotation>) : String {
        val message = StringBuilder("분석 결과\n")
        labels.forEach {
                message.append(String.format(Locale.US, "%.3f: %s", it.score, it.description))
                message.append("\n")
            }
            return message.toString()
    }


}