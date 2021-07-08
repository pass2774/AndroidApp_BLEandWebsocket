package com.example.androidapp_bleandwebsocket

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.SystemClock

import android.graphics.Color
import android.graphics.DashPathEffect
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

import com.example.androidapp_bleandwebsocket.databinding.FragmentChartBinding
import android.widget.Toast
import androidx.databinding.DataBindingUtil.setContentView
import android.widget.Button
import com.example.androidapp_bleandwebsocket.main.MainActivity
import java.util.*
import kotlin.math.sin

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentChart.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentChart : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var mContext: Context


    // For Chart
    var Chart_linedataset: LineDataSet? = null
    // 그래프 data 생성 -> 최종 입력 데이터
    var Chart_data: LineData? = null


    var chart_y_dummy: Float = 0.0f

    override fun onAttach(context: Context){
        super.onAttach(context)
        mContext = context
    }

    var isrunning = false
    private var _binding: FragmentChartBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private fun InitChartSetting(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_chart, container, false)
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.startButton.text = "Graph YEE!"

        Chart_linedataset = createLineDataSet()
        Chart_data = LineData(Chart_linedataset)
        // (~chart~).xml에 배치된 lineChart에 데이터 연결
        binding.lineChart.data = Chart_data

        binding.startButton.setOnClickListener {
            if (isrunning == false) {
                isrunning = true
                binding.startButton.text = "그래프 구현중"
                binding.startButton.isClickable = false
                val thread = ThreadClass()
//                for(i in 0..1) {
                    thread.start()
//                }
            }
        }
        return view
    }

    override fun onDestroyView(){
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentChart.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentChart().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun createLineDataSet() :LineDataSet{
        //            var entries: ArrayList<Entry> = ArrayList()
//            // Entry 배열 초기값 입력
//            entries.add(Entry(0F , 0F))
//            // 그래프 구현을 위한 LineDataSet 생성
//            var dataset: LineDataSet = LineDataSet(entries, "input")
        var linedataset: LineDataSet = LineDataSet(null, "fake signal")
        linedataset.setLineWidth(1f);
        linedataset.setDrawValues(false);
        linedataset.setValueTextColor(Color.WHITE);
        linedataset.setColor(Color.WHITE);
        linedataset.setMode(LineDataSet.Mode.LINEAR);
        linedataset.setDrawCircles(false);
        linedataset.setHighLightColor(Color.rgb(190, 190, 190));
        return linedataset
    }

    inner class ThreadClass : Thread() {
        override fun run() {
            val input = Array<Double>(200,{Math.random()})
            val input_activity = Array<Double>(100,{Math.random()})

            (mContext as MainActivity).runOnUiThread {
                // 그래프 생성
                binding.lineChart.animateXY(1, 1)
            }

            for (i in 0 until input.size){
//                SystemClock.sleep(5)
                SystemClock.sleep(30)
                Chart_data?.addEntry(Entry(i.toFloat(), sin(i.toFloat()/3.0f)+1.0f+chart_y_dummy), 0)
                Chart_data?.notifyDataChanged()
                binding.lineChart.notifyDataSetChanged()
                binding.lineChart.invalidate()
            }
            chart_y_dummy=chart_y_dummy+1.0f
            binding.startButton.text = "난수 생성 시작"
            binding.startButton.isClickable = true
            isrunning = false
        }
    }

}