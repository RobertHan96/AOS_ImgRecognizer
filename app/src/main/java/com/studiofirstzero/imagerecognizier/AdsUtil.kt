package com.studiofirstzero.imagerecognizier

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.*
import kotlin.math.log

class AdsUtil() {
    fun setAdsViewEventHandler(addView: AdView) {
        addView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("log", "광고 불러오기 성공")
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                Log.d("log", "광고 불러오기 실패${adError}")

            }
        }
    }
}