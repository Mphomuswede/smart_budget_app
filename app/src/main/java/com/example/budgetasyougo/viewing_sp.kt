package com.example.budgetasyougo

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
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

class viewing_sp : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var categoryKey: String
    private lateinit var userEmail: String
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewing_sp)

        barChart = findViewById(R.id.categoryBarChart)

        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""
        categoryKey = "categories_$userEmail"

        val categories = loadCategories()

        if (categories.isNotEmpty()) {
            setupBarChart(categories)
            evaluateSpending(categories)

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



    private fun evaluateSpending(categories: List<JSONObject>) {
        val feedbackTextView = findViewById<TextView>(R.id.spendingFeedback)
        val builder = StringBuilder()
        var hasOverspending = false

        for (category in categories) {
            val name = category.optString("name", "Unknown")
            val budget = category.optDouble("budget", 0.0)
            val spent = category.optDouble("spent", 0.0)

            if (budget <= 0) continue

            val percentSpent = (spent / budget) * 100

            builder.append("• $name\n")
            builder.append("  Budget: ${currencyFormatter.format(budget)}\n")
            builder.append("  Spent: ${currencyFormatter.format(spent)} (${String.format("%.1f", percentSpent)}%)\n")

            when {
                percentSpent >= 100 -> {
                    builder.append("  ❌ You overspent!\n\n")
                    hasOverspending = true
                }
                percentSpent >= 85 -> {
                    builder.append("  ⚠️ Nearly reached your budget.\n\n")
                    hasOverspending = true
                }
                else -> {
                    builder.append("  ✅ Spending is under control.\n\n")
                }
            }
        }

        feedbackTextView.text = builder.toString().trim()
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
