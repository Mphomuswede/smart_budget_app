package com.example.budgetasyougo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

class category : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var sharedPref: SharedPreferences
    private var userEmail: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.category_page)

        prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: "non"

        val nameField = findViewById<EditText>(R.id.categoryName)
        val descField = findViewById<EditText>(R.id.categoryDescription)
        val minGoalField = findViewById<EditText>(R.id.categoryMinGoal)
        val budgetField = findViewById<EditText>(R.id.categoryBudget)
        val saveButton = findViewById<Button>(R.id.saveCategoryButton)
        val chart = findViewById<BarChart>(R.id.categoryChart)
        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
        val rootLayout = findViewById<View>(R.id.categoryLayout)

        totals()
        val mainKey = "balance_$userEmail"
        checkAndPromptInitialBudget(mainKey, rootLayout)

        saveButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val desc = descField.text.toString().trim()
            val minGoalText = minGoalField.text.toString().trim()
            val budgetText = budgetField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Category name is required"
                return@setOnClickListener
            }
            if (minGoalText.isEmpty()) {
                minGoalField.error = "Minimum goal is required"
                return@setOnClickListener
            }
            if (budgetText.isEmpty()) {
                budgetField.error = "Maximum goal is required"
                return@setOnClickListener
            }

            val minGoal = minGoalText.toDoubleOrNull() ?: 0.0
            val budget = budgetText.toDoubleOrNull() ?: 0.0

            if (budget <= 0) {
                budgetField.error = "Budget must be greater than zero"
                return@setOnClickListener
            }
            if (minGoal > budget) {
                minGoalField.error = "Min goal cannot exceed max goal"
                return@setOnClickListener
            }

            val mainBudget = prefs.getFloat(mainKey, -1f).toDouble()
            if (mainBudget < 0) {
                AlertDialog.Builder(this)
                    .setTitle("No Main Budget")
                    .setMessage("Please set your main budget first.")
                    .setPositiveButton("Set Budget") { _, _ ->
                        showTopUpDialog(budget, mainKey, rootLayout) {
                            saveCategory(name, desc, minGoal, budget, chart, pieChart, rootLayout, nameField, descField, minGoalField, budgetField)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnClickListener
            }

            if (budget > mainBudget) {
                AlertDialog.Builder(this)
                    .setTitle("Budget Exceeded")
                    .setMessage("Your category budget exceeds the remaining main budget.")
                    .setPositiveButton("Top Up") { _, _ ->
                        showTopUpDialog(budget - mainBudget, mainKey, rootLayout) {
                            saveCategory(name, desc, minGoal, budget, chart, pieChart, rootLayout, nameField, descField, minGoalField, budgetField)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnClickListener
            }

            saveCategory(name, desc, minGoal, budget, chart, pieChart, rootLayout, nameField, descField, minGoalField, budgetField)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveCategory(
        name: String, desc: String, minGoal: Double, budget: Double,
        chart: BarChart, pieChart: PieChart, rootLayout: View,
        nameField: EditText, descField: EditText, minGoalField: EditText, budgetField: EditText
    ) {
        val key = "categories_$userEmail"
        val existingData = prefs.getString(key, "[]")
        val categoryArray = JSONArray(existingData)

        val category = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("name", name)
            put("description", desc)
            put("minGoal", minGoal)
            put("budget", budget)
            put("spent", 0.0)
            put("createdAt", LocalDateTime.now().toString())
        }

        categoryArray.put(category)
        prefs.edit().putString(key, categoryArray.toString()).apply()

        val mainKey = "balance_$userEmail"
        val currentMainBudget = prefs.getFloat(mainKey, 0f)
        prefs.edit().putFloat(mainKey, (currentMainBudget - budget).toFloat()).apply()

        Snackbar.make(rootLayout, "Category saved!", Snackbar.LENGTH_LONG).show()

        nameField.text.clear()
        descField.text.clear()
        minGoalField.text.clear()
        budgetField.text.clear()
        totals()
    }

    private fun totals() {
        val categoryKey = "categories_$userEmail"
        val categoryArray = JSONArray(prefs.getString(categoryKey, "[]"))
        var total = 0.0
        for (i in 0 until categoryArray.length()) {
            total += categoryArray.getJSONObject(i).optDouble("budget", 0.0)
        }
        findViewById<TextView>(R.id.totalSpendingView).text = "Total Budgeted: R %.2f".format(total)
    }

    private fun checkAndPromptInitialBudget(mainKey: String, rootLayout: View) {
        if (prefs.getFloat(mainKey, -1f) < 0f) {
            AlertDialog.Builder(this)
                .setTitle("Set Initial Budget")
                .setMessage("Set your starting budget now?")
                .setPositiveButton("Yes") { _, _ -> showTopUpDialog(0.0, mainKey, rootLayout) {} }
                .setNegativeButton("No", null)
                .show()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showTopUpDialog(requiredBudget: Double, mainKey: String, rootLayout: View, onComplete: () -> Unit) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_top_up, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        val topUpAmountField = view.findViewById<EditText>(R.id.topUpAmount)
        view.findViewById<Button>(R.id.saveTopUpButton).setOnClickListener {
            val topUp = topUpAmountField.text.toString().toFloatOrNull() ?: 0f
            val current = prefs.getFloat(mainKey, 0f)
            prefs.edit().putFloat(mainKey, current + topUp).apply()
            dialog.dismiss()
            onComplete()
        }
        view.findViewById<Button>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun back_homes(view: View) {
        startActivity(Intent(this, dashboard::class.java))
        finish()
    }
}
