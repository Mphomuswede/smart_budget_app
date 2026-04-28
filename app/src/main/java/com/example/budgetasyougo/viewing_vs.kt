package com.example.budgetasyougo

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class viewing_vs : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var categoryKey: String
    private lateinit var userEmail: String
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewing_vs)

        pieChart = findViewById(R.id.categoryPieChart)
        barChart = findViewById(R.id.categoryBarChart)

        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""
        categoryKey = "categories_$userEmail"

        val categories = loadCategories()

        if (categories.isNotEmpty()) {
            setupPieChart(categories)
            setupBarChart(categories)
        } else {
            pieChart.clear()
            barChart.clear()
        }
    }

    private fun loadCategories(): List<JSONObject> {
        val jsonStr = prefs.getString(categoryKey, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getJSONObject(i))
        }
        return list
    }

    private fun setupPieChart(categories: List<JSONObject>) {
        val entries = mutableListOf<PieEntry>()

        for (category in categories) {
            val name = category.optString("name", "Unknown")
            val budget = category.optDouble("budget", 0.0)
            val spent = category.optDouble("spent", 0.0)

            if (spent > 0) {
                val label = "$name[${currencyFormatter.format(budget)}]"
                entries.add(PieEntry(spent.toFloat(), label))
            }
        }

        val dataSet = PieDataSet(entries, "Spent per Category").apply {
            colors = listOf(
                Color.parseColor("#FF5722"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#009688"),
                Color.parseColor("#E91E63")
            )
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            sliceSpace = 3f
        }

        pieChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return currencyFormatter.format(value.toDouble())
                    }
                })
            }
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(false)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000)
            invalidate()
        }
    }


    private fun setupBarChart(categories: List<JSONObject>) {
        val barEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        for ((index, category) in categories.withIndex()) {
            val name = category.optString("name", "Unknown")
            val budget = category.optDouble("budget", 0.0).toFloat()
            val spent = category.optDouble("spent", 0.0).toFloat()
            val remaining = (budget - spent).coerceAtLeast(0f)

            // Stacked entry: [spent, remaining]
            barEntries.add(BarEntry(index.toFloat(), floatArrayOf(spent, remaining)))
            labels.add("$name")
        }

        val stackedSet = BarDataSet(barEntries, "Spent vs Remaining").apply {
            setColors(
                Color.parseColor("#F44336"), // Spent - Red
                Color.parseColor("#4CAF50")  // Remaining - Green
            )
            stackLabels = arrayOf("Spent", "Remaining")
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }

        val barData = BarData(stackedSet).apply {
            barWidth = 0.5f
        }

        barChart.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = true

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 10f
                labelRotationAngle = -15f
            }

            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }



    fun backing(view: View) {
        val intent = Intent(this, dashboard::class.java)
        startActivity(intent)
        finish()
    }
}
