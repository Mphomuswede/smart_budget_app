package com.example.budgetasyougo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class gaming : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementAdapter
    private lateinit var prefs: SharedPreferences
    private lateinit var expensePrefs: SharedPreferences
    private lateinit var categoryKey: String
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaming)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""
        categoryKey = "categories_$userEmail"
        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)
        expensePrefs = getSharedPreferences("StructuredExpenses", MODE_PRIVATE)

        recyclerView = findViewById(R.id.achievementRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val categories = loadCategories()
        val expenses = loadExpenses()

        val achievements = getUnlockedAchievements(categories, expenses)
        adapter = AchievementAdapter(achievements)
        recyclerView.adapter = adapter
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

    private fun loadExpenses(): List<JSONObject> {
        val jsonStr = expensePrefs.getString(userEmail, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getJSONObject(i))
        }
        return list
    }

    private fun getUnlockedAchievements(categories: List<JSONObject>, expenses: List<JSONObject>): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()

        val categoryCount = categories.size
        val totalBudget = categories.sumOf { it.optDouble("budget", 0.0) }
        val totalSpent = categories.sumOf { it.optDouble("spent", 0.0) }
        val expenseCount = expenses.size

        // Calculate unique days logged
        val uniqueDays = expenses.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.optLong("date", 0)
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size

        fun add(title: String, desc: String, img: Int) {
            unlocked.add(Achievement(title, desc, img))
        }

        // Category Achievements
        if (categoryCount >= 1) add("First Step", "Created your first category!", R.drawable.baa)
        if (categoryCount >= 5) add("Budget Boss", "Managing 5+ categories!", R.drawable.baa)

        // Logging Achievements (Consistency)
        if (expenseCount >= 1) add("Logged In", "Logged your first expense!", R.drawable.ic_coin)
        if (uniqueDays >= 3) add("Habit Builder", "Logged expenses on 3 different days!", R.drawable.ic_coin)
        if (uniqueDays >= 7) add("Consistency King", "Loggings across a full week!", R.drawable.ic_coin)

        // Performance Achievements (Meeting Goals)
        val categoriesInGoalRange = categories.count { 
            val spent = it.optDouble("spent", 0.0)
            spent >= it.optDouble("minGoal", 0.0) && spent <= it.optDouble("budget", 0.0) && spent > 0
        }
        if (categoriesInGoalRange >= 1) {
            add("Goal Getter", "Stayed within your goals in at least one category!", R.drawable.baa)
        }
        if (categoriesInGoalRange >= 3) {
            add("Triple Threat", "Stayed within goals for 3 categories!", R.drawable.baa)
        }

        if (totalSpent < totalBudget && totalSpent > 0) {
            add("Smart Saver", "Overall spending is under total budget!", R.drawable.baa)
        }

        return unlocked
    }

    fun backing(view: View) {
        finish()
    }

    data class Achievement(
        val title: String,
        val description: String,
        val imageResId: Int
    )
}
