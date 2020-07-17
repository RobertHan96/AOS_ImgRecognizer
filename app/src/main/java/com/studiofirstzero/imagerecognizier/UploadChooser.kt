package com.studiohana.facerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studiofirstzero.imagerecognizier.R
import kotlinx.android.synthetic.main.upload_chooser.*

class UploadChooser: BottomSheetDialogFragment() {
    interface NotifierInterface{
        fun cameraOnClick()
        fun galleryOnClick()
    }

    var uploadChooserNotifierInterface : NotifierInterface? = null
    fun addNotifier(listener : NotifierInterface){
        uploadChooserNotifierInterface = listener
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_chooser, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()
    }

    private fun setupListener(){
        uploadCamera.setOnClickListener {
            uploadChooserNotifierInterface?.cameraOnClick()
        }
        uploadGallery.setOnClickListener {
            uploadChooserNotifierInterface?.galleryOnClick()
        }
    }
}