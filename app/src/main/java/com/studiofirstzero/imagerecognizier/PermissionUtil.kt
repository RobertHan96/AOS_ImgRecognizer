package com.studiohana.facerecognizer

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtil(){
    val CAMERA_PERMISSION_REQUEST = 1000
    val GALLERY_PERMISSION_REQUEST = 1001
    val FILE_NAME = "picture.jpg"

    fun requestPermission(
        activity: Activity, requestCode: Int, vararg permissions: String): Boolean
        {
            var granted = true
            val permissionNeeded = ArrayList<String>()

            permissions.forEach {
                val permissionCheck = ContextCompat.checkSelfPermission(activity, it)
                val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
                granted = granted and hasPermission
                if (!hasPermission) {
                    permissionNeeded.add(it)
                }

            }

            // requestPermission함수에서 원하는 형태(String)으로 바꾸기 위해서
            // toTypedArrary() 함수를 호출
            if (granted) return true
            else {
                ActivityCompat.requestPermissions(activity, permissionNeeded.toTypedArray(), requestCode)
                return false
            }
        }

    // 입력된 코드와 인자들을 기준으로 해당 권한 요청이 실제로 승인됐던 권한인지 최종확인하는 함수
    fun permissionGranted(requestCode: Int, permissionCode : Int, grantResults: IntArray):Boolean {
        return requestCode == permissionCode && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    //    권한 설정 및 인텐트 접근 관련
    fun checkCameraPermission(act: Activity){
        if (PermissionUtil().requestPermission(
                act,
                CAMERA_PERMISSION_REQUEST,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
        {
//            openCamera()
        }
    }

    fun checkGalleryPermission(act: Activity){
        if (PermissionUtil().requestPermission(
                act, GALLERY_PERMISSION_REQUEST,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
        {
//            openGallery()
        }
    }

}