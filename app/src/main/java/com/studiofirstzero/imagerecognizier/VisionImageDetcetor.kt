package com.studiofirstzero.imagerecognizier

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.main_analyize_view.*

class VisionImageDetcetor {
    fun logResult(results: ArrayList<VisionDetectResult>) {
        for(result in results){
            Log.d("VisionDetector", "$result")
        }
    }

     fun detectLabels(bitmap : Bitmap) : ArrayList<VisionDetectResult> {
        var detectResults : ArrayList<VisionDetectResult> = ArrayList()
        val visionImage = FirebaseVisionImage.fromBitmap(bitmap)
        val labeler = FirebaseVision.getInstance().cloudImageLabeler
        labeler.processImage(visionImage).addOnSuccessListener { labels->
            for (label in labels) {
                val name = label.text
                val confidence = label.confidence
                val visionDetectedResult = VisionDetectResult(name, confidence)
                Log.d("VisionDetector", "$visionDetectedResult")
                detectResults.add(visionDetectedResult)
            }

        }.addOnFailureListener { e ->
            Log.d("VisionDetector", "$e")
        }
        return  detectResults
    }

    fun detectLandmarks(bitmap : Bitmap) : ArrayList<VisionDetectResult> {
        var detectResults : ArrayList<VisionDetectResult> = ArrayList()
        val visionImage = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance().visionCloudLandmarkDetector
        val result = detector.detectInImage(visionImage)
            .addOnSuccessListener { landmarks ->
                for (landmark in landmarks) {
                    val name = landmark.landmark
                    val confidence = landmark.confidence
                    val visionDetectResult = VisionDetectResult(name, confidence)
                    detectResults.add(visionDetectResult)
                }

            }
            .addOnFailureListener { e ->
                Log.d("VisionDetector", "$e")
            }
        return  detectResults
    }

}