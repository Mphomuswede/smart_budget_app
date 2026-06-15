package com.example.budgetasyougo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class viewing_vs : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var periodSpinner: Spinner
    private lateinit var analysisSummary: TextView
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var expensePrefs: android.content.SharedPreferences
    private lateinit var userEmail: String
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewing_vs)

        pieChart = findViewById(R.id.categoryPieChart)
        barChart = findViewById(R.id.categoryBarChart)
        periodSpinner = findViewById(R.id.periodSpinner)
        analysisSummary = findViewById(R.id.analysisSummary)

        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)
        expensePrefs = getSharedPreferences("StructuredExpenses", MODE_PRIVATE)
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""

        setupSpinner()
    }

    private fun setupSpinner() {
        val periods = arrayOf("Past Day", "Past Week", "Past Month", "All Time")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter

        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateVisuals(periods[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        periodSpinner.setSelection(2) // Default to Past Month
    }

    private fun updateVisuals(period: String) {
        val categories = loadCategories()
        val expenses = loadFilteredExpenses(period)

        val spentMap = mutableMapOf<String, Double>()
        for (i in 0 until expenses.length()) {
            val exp = expenses.getJSONObject(i)
            val cat = exp.getString("category")
            spentMap[cat] = spentMap.getOrDefault(cat, 0.0) + exp.getDouble("amount")
        }

        setupBarChart(categories, spentMap)
        setupPieChart(spentMap)
        updateAnalysisText(categories, spentMap, period)
    }

    private fun loadCategories(): List<JSONObject> {
        val jsonStr = prefs.getString("categories_$userEmail", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getJSONObject(i))
        }
        return list
    }

    private fun loadFilteredExpenses(period: String): JSONArray {
        val jsonStr = expensePrefs.getString(userEmail, "[]") ?: "[]"
        val allExpenses = JSONArray(jsonStr)
        val filtered = JSONArray()
        val now = System.currentTimeMillis()

        val limit = when (period) {
            "Past Day" -> TimeUnit.DAYS.toMillis(1)
            "Past Week" -> TimeUnit.DAYS.toMillis(7)
            "Past Month" -> TimeUnit.DAYS.toMillis(30)
            else -> Long.MAX_VALUE
        }

        for (i in 0 until allExpenses.length()) {
            val exp = allExpenses.getJSONObject(i)
            val timestamp = exp.optLong("date", 0)
            if (now - timestamp <= limit || period == "All Time") {
                filtered.put(exp)
            }
        }
        return filtered
    }

    private fun setupBarChart(categories: List<JSONObject>, spentMap: Map<String, Double>) {
        if (categories.isEmpty()) {
            barChart.clear()
            return
        }

        val spentEntries = mutableListOf<BarEntry>()
        val minEntries = mutableListOf<BarEntry>()
        val maxEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        for ((index, cat) in categories.withIndex()) {
            val name = cat.getString("name")
            val spent = spentMap[name] ?: 0.0
            val minGoal = cat.optDouble("minGoal", 0.0)
            val maxGoal = cat.optDouble("budget", 0.0)

            spentEntries.add(BarEntry(index.toFloat(), spent.toFloat()))
            minEntries.add(BarEntry(index.toFloat(), minGoal.toFloat()))
            maxEntries.add(BarEntry(index.toFloat(), maxGoal.toFloat()))
            labels.add(name)
        }

        val spentSet = BarDataSet(spentEntries, "Actual Spent").apply { color = Color.parseColor("#4CAF50") }
        val minSet = BarDataSet(minEntries, "Min Goal").apply { color = Color.parseColor("#FFEE58") }
        val maxSet = BarDataSet(maxEntries, "Max Goal").apply { color = Color.parseColor("#EF5350") }

        val barData = BarData(spentSet, minSet, maxSet).apply {
            barWidth = 0.2f
        }

        barChart.apply {
            data = barData
            description.isEnabled = false
            
            // groupSpace: 0.25, barSpace: 0.05, barWidth: 0.2 => (0.2+0.05)*3 + 0.25 = 1.00
            groupBars(0f, 0.25f, 0.05f) 
            
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                isGranularityEnabled = true
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = categories.size.toFloat()
                labelRotationAngle = -45f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#33FFFFFF")
            }
            axisRight.isEnabled = false
            legend.textColor = Color.WHITE
            legend.isWordWrapEnabled = true
            animateY(1000)
            invalidate()
        }
    }

    private fun setupPieChart(spentMap: Map<String, Double>) {
        if (spentMap.isEmpty()) {
            pieChart.clear()
            return
        }
        val entries = spentMap.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.WHITE)
            animateXY(1000, 1000)
            invalidate()
        }
    }

    private fun updateAnalysisText(categories: List<JSONObject>, spentMap: Map<String, Double>, period: String) {
        if (categories.isEmpty()) {
            analysisSummary.text = "No categories found. Start by creating a budget!"
            return
        }

        var onTrackCount = 0
        var overspentCount = 0
        var underMinCount = 0

        for (cat in categories) {
            val name = cat.getString("name")
            val spent = spentMap[name] ?: 0.0
            val min = cat.optDouble("minGoal", 0.0)
            val max = cat.optDouble("budget", 0.0)

            when {
                spent > max -> overspentCount++
                spent < min -> underMinCount++
                else -> onTrackCount++
            }
        }

        val summary = StringBuilder()
        summary.append("Analysis for $period:\n")
        summary.append("✅ On Track: $onTrackCount categories\n")
        if (overspentCount > 0) summary.append("⚠️ Over Limit: $overspentCount categories\n")
        if (underMinCount > 0) summary.append("ℹ️ Below Min Goal: $underMinCount categories\n")
        
        if (overspentCount == 0 && onTrackCount > 0) {
            summary.append("\nGreat job! You are staying within your goals.")
        } else if (overspentCount > 0) {
            summary.append("\nConsider reviewing your spending in over-limit categories.")
        }

        analysisSummary.text = summary.toString()
    }

    fun backing(view: View) {
        finish()
    }
}
