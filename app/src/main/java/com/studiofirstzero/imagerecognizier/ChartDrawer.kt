package com.studiofirstzero.imagerecognizier

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.main_analyize_view.*
import java.io.IOException

class ChartDrawer() {
    fun isValidDetectionResultArrary(results : ArrayList<VisionDetectResult>) : Boolean {
        return results.size >= 3
    }

    fun getChartEntries (labelArrary: ArrayList<VisionDetectResult>) : BarData{
        val entries = ArrayList<BarEntry>()
        for (i in 0..2) {
            val label = labelArrary.get(i)
            val entry = BarEntry(i.toFloat(), label.confidence * 100)
            entries.add(entry)
            }
        val colors : ArrayList<Int> = arrayListOf(ColorTemplate.rgb("#ff77b2"), Color.BLUE, Color.DKGRAY)
        val barDataSet = BarDataSet(entries, "일치율")
        barDataSet.colors = colors
        val data = BarData(barDataSet)

        return data
    }

    fun getChartIndex(labelArrary: ArrayList<VisionDetectResult>) : ArrayList<String> {
        val labels = ArrayList<String>()
        for (i in 0..2) {
            val label = labelArrary.get(i)
            labels.add("${label.name}")
        }
        return labels
    }

}