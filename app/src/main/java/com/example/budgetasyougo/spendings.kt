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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class spendings : AppCompatActivity() {

    private lateinit var mainBudgetText: TextView
    private lateinit var availableBalanceText: TextView
    private lateinit var spendingText: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var sliderView: ViewPager2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spendings)

        mainBudgetText = findViewById(R.id.mainBudgetText)
        availableBalanceText = findViewById(R.id.availableBalanceText)
        spendingText = findViewById(R.id.spendingText)
       // recyclerView = findViewById(R.id.categoryRecyclerView)
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val balanceKey = "balance_$userEmail"

        val currentBalance = prefs.getFloat(balanceKey, -1f)
        val rootView = findViewById<View>(android.R.id.content)
        val balance = getUserBalance()
        if (currentBalance < 0f) {
            // first time, no budget saved yet
            askToSetInitialBudget(balanceKey, rootView)
        } else if (currentBalance == 0f) {
            // balance zero, suggest topping up
            askToSetInitialBudget(balanceKey, rootView)
        } else {
            // balance exists, just show or proceed
            findViewById<TextView>(R.id.availableBalanceText).text = " %.2f".format(currentBalance)+" ZAR"
        }

        findViewById<TextView>(R.id.mainBudgetText).text = ""+balance+" ZAR"

     totals()
        val pieChart = findViewById<PieChart>(R.id.categoryPieChart)

        showPieChart(pieChart)
        // Set up the slider
        sliderView = findViewById(R.id.sliderViews)

        val cardItems = listOf(
            dashboard.CardInfo(
                R.drawable.smart,
                R.drawable.smartlogo,
                "Track Spending",
                "Tracking your spending helps you understand exactly where your money goes. By staying aware of your daily expenses, you can identify patterns, avoid unnecessary purchases, and take control of your financial future with confidence."
            )
            ,
            dashboard.CardInfo(
                R.drawable.smart,
                R.drawable.smartlogo,
                "Visual Budget",
                "Visualizing your budget lets you quickly see how much you’ve spent, what remains, and where you’re heading. It makes managing money easier by turning numbers into clear insights, helping you stay on track and avoid surprises."
            )
            ,
            dashboard.CardInfo(
                R.drawable.smart,
                R.drawable.smartlogo,
                "Budget Game",
                "Learn how to manage money through fun, interactive challenges. The Budget Game helps you build smart financial habits by turning real-life budgeting scenarios into engaging and rewarding experiences."
            )

        )

        sliderView.adapter = SlideCardAdapter(cardItems)
    }


fun topUps(){
    val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
    val userEmail = sharedPrefs.getString("email", "") ?: ""
    val balanceKey = "balance_$userEmail"

    val currentBalance = prefs.getFloat(balanceKey, -1f)
    val rootView = findViewById<View>(android.R.id.content)
    askToSetInitialBudget(balanceKey, rootView)
}
    private fun showPieChart(pieChart: PieChart) {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        val totalText = findViewById<TextView>(R.id.mainBudgetText).text.toString().replace("[^\\d.]".toRegex(), "")
        val spentText = findViewById<TextView>(R.id.spendingText).text.toString().replace("[^\\d.]".toRegex(), "")

        val totalBudget = totalText.toFloatOrNull() ?: 0f
        val spent = spentText.toFloatOrNull() ?: 0f
        val remaining = (totalBudget - spent).coerceAtLeast(0f)

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        if (spent > 0f) {
            entries.add(PieEntry(spent, "Spent"))
            colors.add(Color.RED)
        }

        if (remaining > 0f) {
            entries.add(PieEntry(remaining, "Remaining"))
            colors.add(Color.GREEN)
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No budget or spending data")
            return
        }

        val dataSet = PieDataSet(entries, "Budget Breakdown").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return format.format(value.toDouble())
                }
            }
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.YELLOW)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setUsePercentValues(false)
        pieChart.centerText = "Total:\n${format.format(totalBudget.toDouble())}"
        pieChart.setCenterTextSize(16f)
        pieChart.invalidate()
        pieChart.visibility = View.VISIBLE
    }



    fun totals(){

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
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

        findViewById<TextView>(R.id.spendingText).text =""+totalCategoryBudget+" ZAR"

    }


    private fun getUserBalance(): Float {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
        return mainSpends.getFloat(userEmail, 0f)
    }

    private fun initializeUserBalance(initialBalance: Float) {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)

        // Check if user has a balance
        if (!mainSpends.contains(userEmail)) {
            mainSpends.edit().putFloat(userEmail, initialBalance.toFloat()).apply()
        }
    }
    private fun addToUserBalance(amountToAdd: Float) {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)

        val currentBalance = mainSpends.getFloat(userEmail, 0f)
        val newBalance = currentBalance + amountToAdd

        mainSpends.edit().putFloat(userEmail, newBalance).apply()
    }

    private fun askToSetInitialBudget(balanceKey: String, rootView: View) {
        AlertDialog.Builder(this)
            .setTitle("Set Initial Budget")
            .setMessage("Do you want to top up now")
            .setPositiveButton("Yes") { _, _ ->
                // Show top-up dialog to set initial balance
                showTopUpDialog(balanceKey, rootView)
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun showTopUpDialog(balanceKey: String, rootView: View) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_top_up, null)

        val currentBalanceText = view.findViewById<TextView>(R.id.currentBalance)
        val topUpAmountField = view.findViewById<EditText>(R.id.topUpAmount)
        val saveButton = view.findViewById<Button>(R.id.saveTopUpButton)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        // Use budgetAppPrefs here
        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val currentBalance = prefs.getFloat(balanceKey, 0f)
        currentBalanceText.text = " %.2f".format(currentBalance) +" ZAR"
        initializeUserBalance(currentBalance)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        saveButton.setOnClickListener {
            val topUp = topUpAmountField.text.toString().toFloatOrNull()
            if (topUp == null || topUp <= 0f) {
                topUpAmountField.error = "Please enter a valid amount"
                return@setOnClickListener
            }
            val newBalance = currentBalance + topUp
            prefs.edit().putFloat(balanceKey, newBalance).apply()
            addToUserBalance(topUp)
            Snackbar.make(rootView, "Main budget updated to R %.2f".format(newBalance), Snackbar.LENGTH_LONG).show()

            // Update dashboard balance display too
            findViewById<TextView>(R.id.availableBalanceText).text = "%.2f".format(newBalance)+" ZAR"

            val balance = getUserBalance()
            findViewById<TextView>(R.id.availableBalanceText).text = ""+balance+" ZAR"

            dialog.dismiss()

            val intent = Intent(this, spendings::class.java)
            startActivity(intent)
            finish()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }












    data class CategoryData(
        val title: String,
        val budget: Double,
        val spent: Double
    )

    fun back_home(view: View) {


        val intent = Intent(this, dashboard::class.java)
        startActivity(intent)
        finish()
    }

    fun tops(view: View) {
        topUps()

    }

    fun clears(view: View) {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("email", "") ?: ""
        val mainKey = "balance_$userEmail"
        val categoryKey = "categories_$userEmail"

        // Remove both keys
        prefs.edit()
            .remove(mainKey)
            .remove(categoryKey)
            .apply()

        val mainSpends = getSharedPreferences("main_spends", Context.MODE_PRIVATE)
        mainSpends.edit().remove(userEmail).apply()

        //Toast.makeText(this, "User balance and categories cleared!", Toast.LENGTH_LONG).show()

        val intent = Intent(this, spendings::class.java)
        startActivity(intent)
        finish()
    }



}
