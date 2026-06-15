package com.example.budgetasyougo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class spendings : AppCompatActivity() {

    private lateinit var mainBudgetValue: TextView
    private lateinit var availableBalanceValue: TextView
    private lateinit var spendingValue: TextView
    private lateinit var sliderView: ViewPager2

    private lateinit var searchView: SearchView
    private lateinit var btnAll: Button
    private lateinit var btnSpent: Button
    private lateinit var btnRemaining: Button
    private lateinit var btnOverBudget: Button

    private var fullCategoryList = mutableListOf<CategoryData>()
    private var filteredList = mutableListOf<CategoryData>()
    private var currentFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spendings)

        mainBudgetValue = findViewById(R.id.mainBudgetValue)
        availableBalanceValue = findViewById(R.id.availableBalanceText) // Using the ID from XML
        spendingValue = findViewById(R.id.spendingText) // Using the ID from XML

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val balanceKey = "balance_$userEmail"

        val currentBalance = prefs.getFloat(balanceKey, 0f)
        availableBalanceValue.text = "R %.2f".format(currentBalance)

        val balance = getUserBalance()
        mainBudgetValue.text = "R %.2f".format(balance)

        sliderView = findViewById(R.id.sliderViews)
        setupSearchAndFilters()
        loadCategories()
        
        // Initial summary update
        updateSummary(fullCategoryList)

        val cardItems = listOf(
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Track Spending", "Review your budget categories."),
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Visual Budget", "See your charts."),
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Budget Game", "Earn rewards!")
        )
        sliderView.adapter = SlideCardAdapter(cardItems)
    }

    private fun setupSearchAndFilters() {
        searchView = findViewById(R.id.searchView)
        btnAll = findViewById(R.id.btnAll)
        btnSpent = findViewById(R.id.btnSpent)
        btnRemaining = findViewById(R.id.btnRemaining)
        btnOverBudget = findViewById(R.id.btnOverBudget)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(currentFilter, newText ?: "")
                return true
            }
        })

        btnAll.setOnClickListener { filterList("ALL", searchView.query.toString()) }
        btnSpent.setOnClickListener { filterList("SPENT", searchView.query.toString()) }
        btnRemaining.setOnClickListener { filterList("REMAINING", searchView.query.toString()) }
        btnOverBudget.setOnClickListener { filterList("OVER", searchView.query.toString()) }
    }

    private fun loadCategories() {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val categoryKey = "categories_$userEmail"

        val json = prefs.getString(categoryKey, "[]") ?: "[]"
        val array = JSONArray(json)

        fullCategoryList.clear()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            // FIXED: Using "name" instead of "title"
            val name = obj.optString("name", "Unnamed")
            val budget = obj.optDouble("budget", 0.0)
            val spent = obj.optDouble("spent", 0.0)
            fullCategoryList.add(CategoryData(name, budget, spent))
        }
        filteredList = fullCategoryList.toMutableList()
        updateUI(filteredList)
    }

    private fun filterList(filter: String, searchText: String) {
        currentFilter = filter
        filteredList = fullCategoryList.filter {
            val matchesSearch = it.name.lowercase().contains(searchText.lowercase())
            val matchesFilter = when (filter) {
                "SPENT" -> it.spent > 0
                "REMAINING" -> it.budget > it.spent
                "OVER" -> it.spent > it.budget
                else -> true
            }
            matchesSearch && matchesFilter
        }.toMutableList()

        updateUI(filteredList)
    }

    private fun updateUI(list: List<CategoryData>) {
        updatePieFromFiltered(list)
        updateSummary(list)
    }

    private fun updateSummary(list: List<CategoryData>) {
        val totalSpent = list.sumOf { it.spent }
        spendingValue.text = "R %.2f".format(totalSpent)
    }

    private fun updatePieFromFiltered(list: List<CategoryData>) {
        val spent = list.sumOf { it.spent }.toFloat()
        val remaining = list.sumOf { (it.budget - it.spent).coerceAtLeast(0.0) }.toFloat()

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (spent > 0) {
            entries.add(PieEntry(spent, "Spent"))
            colors.add(Color.parseColor("#EF5350"))
        }
        if (remaining > 0) {
            entries.add(PieEntry(remaining, "Available"))
            colors.add(Color.parseColor("#66BB6A"))
        }

        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
        if (entries.isEmpty()) {
            pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Budget Breakdown"
        pieChart.setCenterTextColor(Color.WHITE)
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.legend.textColor = Color.WHITE
        pieChart.invalidate()
    }

    data class CategoryData(val name: String, val budget: Double, val spent: Double)

    private fun getUserBalance(): Float {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
        return mainSpends.getFloat(userEmail, 0f)
    }

    fun back_home(view: View) {
        finish() // Goes back to Dashboard
    }

    fun tops(view: View) {
        // Logic for top up
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        showTopUpDialog("balance_$userEmail", findViewById(android.R.id.content))
    }

    private fun showTopUpDialog(balanceKey: String, rootView: View) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_top_up, null)
        val topUpAmount = view.findViewById<EditText>(R.id.topUpAmount)
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val amount = topUpAmount.text.toString().toFloatOrNull() ?: 0f
                val current = prefs.getFloat(balanceKey, 0f)
                prefs.edit().putFloat(balanceKey, current + amount).apply()
                
                // Also update main_spends
                val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                val userEmail = sharedPrefs.getString("email", "") ?: ""
                val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
                val oldMain = mainSpends.getFloat(userEmail, 0f)
                mainSpends.edit().putFloat(userEmail, oldMain + amount).apply()

                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun clears(view: View) {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        
        getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("main_spends", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("StructuredExpenses", Context.MODE_PRIVATE).edit().clear().apply()

        recreate()
    }
}
