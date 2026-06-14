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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class spendings : AppCompatActivity() {

    private lateinit var mainBudgetText: TextView
    private lateinit var availableBalanceText: TextView
    private lateinit var spendingText: TextView
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

        mainBudgetText = findViewById(R.id.mainBudgetText)
        availableBalanceText = findViewById(R.id.availableBalanceText)
        spendingText = findViewById(R.id.spendingText)

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)

        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val balanceKey = "balance_$userEmail"

        val currentBalance = prefs.getFloat(balanceKey, -1f)
        val rootView = findViewById<View>(android.R.id.content)

        val balance = getUserBalance()

        if (currentBalance < 0f || currentBalance == 0f) {
            askToSetInitialBudget(balanceKey, rootView)
        } else {
            availableBalanceText.text = "%.2f ZAR".format(currentBalance)
        }

        mainBudgetText.text = "$balance ZAR"


        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
        showPieChart(pieChart)

        sliderView = findViewById(R.id.sliderViews)

        setupSearchAndFilters()
        loadCategories()

        val cardItems = listOf(
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Track Spending", "Tracking spending..."),
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Visual Budget", "Visualizing budget..."),
            dashboard.CardInfo(R.drawable.smart, R.drawable.smartlogo, "Budget Game", "Learn budgeting...")
        )

        sliderView.adapter = SlideCardAdapter(cardItems)
    }

    // ================= SEARCH + FILTER =================

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

        val json = prefs.getString(categoryKey, "[]")
        val array = JSONArray(json)

        fullCategoryList.clear()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            val title = obj.optString("title", "")
            val budget = obj.optDouble("budget", 0.0)
            val spent = obj.optDouble("spent", 0.0)

            fullCategoryList.add(CategoryData(title, budget, spent))
        }

        filteredList = fullCategoryList.toMutableList()
    }

    private fun filterList(filter: String, searchText: String) {

        currentFilter = filter

        filteredList = fullCategoryList.filter {

            val matchesSearch = it.title.lowercase().contains(searchText.lowercase())

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
    }

    private fun updatePieFromFiltered(list: List<CategoryData>) {

        val spent = list.sumOf { it.spent }.toFloat()
        val remaining = list.sumOf { (it.budget - it.spent).coerceAtLeast(0.0) }.toFloat()

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (spent > 0) {
            entries.add(PieEntry(spent, "Spent"))
            colors.add(Color.RED)
        }

        if (remaining > 0) {
            entries.add(PieEntry(remaining, "Remaining"))
            colors.add(Color.GREEN)
        }

        val dataSet = PieDataSet(entries, "Budget").apply {
            this.colors = colors
            valueTextColor = Color.BLACK
            valueTextSize = 14f
        }

        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }



    private fun showPieChart(pieChart: PieChart) {
        pieChart.setNoDataText("Loading...")
    }



    data class CategoryData(
        val title: String,
        val budget: Double,
        val spent: Double
    )


    private fun getUserBalance(): Float {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
        return mainSpends.getFloat(userEmail, 0f)
    }

    private fun askToSetInitialBudget(balanceKey: String, rootView: View) {
        AlertDialog.Builder(this)
            .setTitle("Set Initial Budget")
            .setMessage("Do you want to top up now?")
            .setPositiveButton("Yes") { _, _ ->
                showTopUpDialog(balanceKey, rootView)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showTopUpDialog(balanceKey: String, rootView: View) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_top_up, null)

        val currentBalanceText = view.findViewById<TextView>(R.id.currentBalance)
        val topUpAmount = view.findViewById<EditText>(R.id.topUpAmount)

        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val currentBalance = prefs.getFloat(balanceKey, 0f)

        currentBalanceText.text = "%.2f ZAR".format(currentBalance)

        AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->

                val amount = topUpAmount.text.toString().toFloatOrNull() ?: 0f

                val newBalance = currentBalance + amount

                prefs.edit().putFloat(balanceKey, newBalance).apply()

                Snackbar.make(rootView, "Updated: R$newBalance", Snackbar.LENGTH_LONG).show()

                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    fun back_home(view: View) {
        startActivity(Intent(this, dashboard::class.java))
        finish()
    }

    fun tops(view: View) {
        askToSetInitialBudget("balance", findViewById(android.R.id.content))
    }

    fun clears(view: View) {
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
        mainSpends.edit().clear().apply()

        recreate()
    }
}