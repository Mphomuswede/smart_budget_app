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
        val budgetField = findViewById<EditText>(R.id.categoryBudget)
        val saveButton = findViewById<Button>(R.id.saveCategoryButton)
        val chart = findViewById<BarChart>(R.id.categoryChart)
        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
        val rootLayout = findViewById<View>(R.id.categoryLayout)
        val backIcon = findViewById<ImageView>(R.id.backIcon)

        // backIcon.setOnClickListener { finish() }
totals()
       // clearUserData()
        val mainKey = "balance_$userEmail"
        checkAndPromptInitialBudget(mainKey, rootLayout)

        saveButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val desc = descField.text.toString().trim()
            val budgetText = budgetField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Category name is required"
                return@setOnClickListener
            }
            if (desc.isEmpty()) {
                descField.error = "Category description is required"
                return@setOnClickListener
            }
            if (budgetText.isEmpty()) {
                budgetField.error = "Budget amount is required"
                return@setOnClickListener
            }

            val budget = budgetText.toDoubleOrNull() ?: 0.0
            if (budget <= 0) {
                budgetField.error = "Budget must be greater than zero"
                return@setOnClickListener
            }

            // Check if there's enough main budget left for this category budget
            val mainBudget = prefs.getFloat(mainKey, -1f).toDouble()
            val categoryKey = "categories_$userEmail"
            val existingCategoriesJson = prefs.getString(categoryKey, "[]")


            val availableBudget =mainBudget - budget//mainBudget - totalCategoryBudget
            Snackbar.make(rootLayout, "Remaining main budget: R %.2f".format(availableBudget), Snackbar.LENGTH_LONG).show()

            if (mainBudget < 0) {
                // No main budget set yet, prompt user to set initial budget first
                AlertDialog.Builder(this)
                    .setTitle("No Main Budget")
                    .setMessage("You have no main budget set. Please set your main budget first.")
                    .setPositiveButton("Set Budget") { _, _ ->
                        showTopUpDialog(budget, mainKey, rootLayout) {
                            saveCategory(name, desc, budget, chart, pieChart, rootLayout, nameField, descField, budgetField)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnClickListener
            }

            initializeUserBalance(mainBudget)
            if (budget > mainBudget) {
                AlertDialog.Builder(this)
                    .setTitle("Budget Exceeded")
                    .setMessage(
                        "Your category budget exceeds the remaining available main budget.\n" +
                                "Remaining main budget: R %.2f".format(mainBudget)
                    )
                    .setPositiveButton("Top Up") { _, _ ->
                        showTopUpDialog(budget - availableBudget, mainKey, rootLayout) {
                            saveCategory(
                                name, desc, budget, chart, pieChart,
                                rootLayout, nameField, descField, budgetField
                            )
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnClickListener
            }


            // All good, save the category
            saveCategory(name, desc, budget, chart, pieChart, rootLayout, nameField, descField, budgetField)
        }
    }

    private fun initializeUserBalance(initialBalance: Double) {
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)

        // Check if user has a balance
        if (!mainSpends.contains(userEmail)) {
            mainSpends.edit().putFloat(userEmail, initialBalance.toFloat()).apply()
        }
    }
    private fun addToUserBalance(amountToAdd: Float) {
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)

        val currentBalance = mainSpends.getFloat(userEmail, 0f)
        val newBalance = currentBalance + amountToAdd

        mainSpends.edit().putFloat(userEmail, newBalance).apply()
    }

    private fun clearUserData() {
        val mainKey = "balance_$userEmail"
        val categoryKey = "categories_$userEmail"

        // Remove both keys
        prefs.edit()
            .remove(mainKey)
            .remove(categoryKey)
            .apply()

        Toast.makeText(this, "User balance and categories cleared!", Toast.LENGTH_LONG).show()
    }




    fun totals(){
    val mainKey = "balance_$userEmail"

    // Check if there's enough main budget left for this category budget
    val mainBudget = prefs.getFloat(mainKey, -1f).toDouble()
    val categoryKey = "categories_$userEmail"
    val existingCategoriesJson = prefs.getString(categoryKey, "[]")
    val categoryArray = JSONArray(existingCategoriesJson)
    var totalCategoryBudget = 0.0
    for (i in 0 until categoryArray.length()) {
        val obj = categoryArray.getJSONObject(i)
        totalCategoryBudget += obj.optDouble("budget", 0.0)
    }

    findViewById<TextView>(R.id.totalSpendingView).text ="Total Budgeted: "+totalCategoryBudget+" ZAR"

}
    // Your existing function: Check if first-time or zero budget and prompt accordingly
    private fun checkAndPromptInitialBudget(mainKey: String, rootLayout: View) {
        val currentBalance = prefs.getFloat(mainKey, -1f)
        if (currentBalance < 0f) {
            AlertDialog.Builder(this)
                .setTitle("Set Initial Budget")
                .setMessage("You have no budget set yet. Would you like to set your starting budget now?")
                .setPositiveButton("Yes") { _, _ ->
                    showTopUpDialog(0.0, mainKey, rootLayout) {}
                }
                .setNegativeButton("No", null)
                .show()
        } else if (currentBalance == 0f) {
            AlertDialog.Builder(this)
                .setTitle("Budget is Zero")
                .setMessage("Your current budget is zero. Would you like to top it up now?")
                .setPositiveButton("Yes") { _, _ ->
                    showTopUpDialog(0.0, mainKey, rootLayout) {}
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveCategory(
        name: String,
        desc: String,
        budget: Double,
        chart: BarChart,
        pieChart: PieChart,
        rootLayout: View,
        nameField: EditText,
        descField: EditText,
        budgetField: EditText
    ) {
        val key = "categories_$userEmail"
        val existingData = prefs.getString(key, "[]")
        val categoryArray = JSONArray(existingData)

        for (i in 0 until categoryArray.length()) {
            val obj = categoryArray.getJSONObject(i)
            if (obj.getString("name").equals(name, ignoreCase = true)) {
                Snackbar.make(rootLayout, "Category already exists", Snackbar.LENGTH_LONG).show()
                return
            }
        }

        val category = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("name", name)
            put("description", desc)
            put("budget", budget)
            put("spent", 0.0)
            put("createdAt", LocalDateTime.now().toString())
        }

        categoryArray.put(category)
        prefs.edit().putString(key, categoryArray.toString()).apply()

        // Subtract the allocated category budget from the main budget here
        val mainKey = "balance_$userEmail"
        val currentMainBudget = prefs.getFloat(mainKey, 0f)
        val updatedMainBudget = (currentMainBudget - budget).toFloat()
        prefs.edit().putFloat(mainKey, updatedMainBudget).apply()

        Snackbar.make(rootLayout, "Category saved successfully!", Snackbar.LENGTH_LONG).show()

        showBarChart(chart, categoryArray)
        showPieChart(pieChart, category)

        nameField.text.clear()
        descField.text.clear()
        budgetField.text.clear()

        totals()
    }

    private fun showBarChart(chart: BarChart, data: JSONArray) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val budget = obj.optDouble("budget", 0.0)
            entries.add(BarEntry(i.toFloat(), budget.toFloat()))
            labels.add(obj.getString("name"))
        }

        val dataSet = BarDataSet(entries, "Budgets in ZAR")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return format.format(value.toDouble())
            }
        }

        val barData = BarData(dataSet)
        chart.data = barData
        chart.description.text = "All Category Budgets"
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun showPieChart(pieChart: PieChart, category: JSONObject) {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val name = category.getString("name")
        val totalBudget = category.optDouble("budget", 0.0)
        val spent = category.optDouble("spent", 0.0)

        val remaining = totalBudget - spent
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        if (spent > 0) {
            entries.add(PieEntry(spent.toFloat(), "Spent"))
            colors.add(Color.RED)
        }

        if (remaining > 0) {
            entries.add(PieEntry(remaining.toFloat(), "Remaining"))
            colors.add(Color.GREEN)
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No budget or spending data")
            return
        }

        val dataSet = PieDataSet(entries, "$name Budget Breakdown")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return format.format(value.toDouble())
            }
        }

        val pieData = PieData(dataSet)

        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setUsePercentValues(false)
        pieChart.invalidate()
        pieChart.visibility = View.VISIBLE
    }

    @SuppressLint("MissingInflatedId")
    private fun showTopUpDialog(
        requiredBudget: Double,
        mainKey: String,
        rootLayout: View,
        onComplete: () -> Unit
    ) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_top_up, null)

        val dialog = android.app.Dialog(this)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentBalanceText = view.findViewById<TextView>(R.id.currentBalance)
        val requiredBudgets = view.findViewById<TextView>(R.id.requiredBalance)
        val topUpAmountField = view.findViewById<EditText>(R.id.topUpAmount)
        val saveButton = view.findViewById<Button>(R.id.saveTopUpButton)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        val currentBalance = prefs.getFloat(mainKey, 0f)

        currentBalanceText.text = "Current Balance: R %.2f\nRequired Budget: R %.2f".format(currentBalance, requiredBudget)
        requiredBudgets.text = "Required budget top-up amount is R %.2f".format(requiredBudget- currentBalance)

        saveButton.setOnClickListener {
            val topUp = topUpAmountField.text.toString().toFloatOrNull()

            if (topUp == null || topUp <= 0f) {
                Snackbar.make(rootLayout, "Enter a valid top-up amount", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val newBalance = currentBalance + topUp - currentBalance

            if (newBalance > requiredBudget) {
                Snackbar.make(rootLayout, "Top-up is less than required budget", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.edit().putFloat(mainKey, newBalance).apply()
            addToUserBalance(topUp)
            dialog.dismiss()
            onComplete()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun back_homes(view: View) {
        val intent = Intent(this, dashboard::class.java)
        startActivity(intent)
        finish()
    }
}
