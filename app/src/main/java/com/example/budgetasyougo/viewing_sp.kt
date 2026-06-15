package com.example.budgetasyougo

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class viewing_sp : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var categoryKey: String
    private lateinit var userEmail: String
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

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
            barChart.clear()
            findViewById<TextView>(R.id.spendingFeedback).text = "No categories found. Create a category to see your spending analysis."
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
                spent > budget -> builder.append("  ❌ Over Budget!\n\n")
                spent >= budget * 0.9 -> builder.append("  ⚠️ Warning: Near limit\n\n")
                else -> builder.append("  ✅ Within Budget\n\n")
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

            barEntries.add(BarEntry(index.toFloat(), floatArrayOf(spent, remaining)))
            labels.add(name)
        }

        val stackedSet = BarDataSet(barEntries, "Spent (Red) vs Remaining (Green)").apply {
            setColors(Color.parseColor("#EF5350"), Color.parseColor("#66BB6A"))
            stackLabels = arrayOf("Spent", "Remaining")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        barChart.apply {
            data = BarData(stackedSet)
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -30f
            }
            axisLeft.textColor = Color.BLACK
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    fun backing(view: View) {
        finish()
    }
}
