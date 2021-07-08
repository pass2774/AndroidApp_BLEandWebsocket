package com.example.androidapp_bleandwebsocket.main

//import android.view.LayoutInflater
//import android.view.View


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
//import com.example.androidapp_bleandwebsocket.R
import com.example.androidapp_bleandwebsocket.databinding.ActivityChartBinding
import android.os.SystemClock


import android.graphics.Color
import android.graphics.DashPathEffect
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChartActivity : AppCompatActivity() {
    var isrunning = false
    private lateinit var binding: ActivityChartBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.startButton.text = "Graph"
//        setContentView(R.layout.chart)
//
        binding.startButton.setOnClickListener {
            if (isrunning == false) {
                isrunning = true
                binding.startButton.text = "그래프 구현중"
                binding.startButton.isClickable = false
                val thread = ThreadClass()
                thread.start()
            }
        }
    }

    inner class ThreadClass : Thread() {
        override fun run() {
            val input = Array<Double>(100,{Math.random()})
            // Entry 배열 생성
            var entries: ArrayList<Entry> = ArrayList()
            // Entry 배열 초기값 입력
            entries.add(Entry(0F , 0F))
            // 그래프 구현을 위한 LineDataSet 생성
            var dataset: LineDataSet = LineDataSet(entries, "input")


            // 그래프 data 생성 -> 최종 입력 데이터
            var data: LineData = LineData(dataset)
            // chart.xml에 배치된 lineChart에 데이터 연결
            binding.lineChart.data = data

            runOnUiThread {
                // 그래프 생성
                binding.lineChart.animateXY(1, 1)
            }

            for (i in 0 until input.size){
                SystemClock.sleep(10)
                data.addEntry(Entry(i.toFloat(), input[i].toFloat()), 0)
                data.notifyDataChanged()
                binding.lineChart.notifyDataSetChanged()
                binding.lineChart.invalidate()
            }
            binding.startButton.text = "난수 생성 시작"
            binding.startButton.isClickable = true
            isrunning = false
        }
    }
}