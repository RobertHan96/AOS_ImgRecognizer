package com.studiofirstzero.imagerecognizier

import android.graphics.Bitmap
import android.util.Log
import android.widget.TextView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage

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