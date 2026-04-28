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

class gaming : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementAdapter
    private lateinit var prefs: SharedPreferences
    private lateinit var categoryKey: String
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaming)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""
        categoryKey = "categories_$userEmail"
        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)

        recyclerView = findViewById(R.id.achievementRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val categories = loadCategories()

        val achievements = getUnlockedAchievements(categories)
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

    private fun getUnlockedAchievements(categories: List<JSONObject>): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()

        val categoryCount = categories.size
        val totalBudget = categories.sumOf { it.optDouble("budget", 0.0) }
        val totalSpent = categories.sumOf { it.optDouble("spent", 0.0) }

        fun add(title: String, desc: String, img: Int) {
            unlocked.add(Achievement(title, desc, img))
        }

        // Achievements based on number of categories
        if (categoryCount >= 1) add("First Category", "Created your first category!", R.drawable.baa)
        if (categoryCount >= 3) add("Triple Tracker", "You now have 3+ categories!", R.drawable.baa)
        if (categoryCount >= 5) add("Budget Boss", "You now manage 5+ categories!", R.drawable.baa)
        if (categoryCount >= 10) add("Planner Master", "10 categories created!", R.drawable.baa)

        // Spending Achievements
        if (categories.any { it.optDouble("spent", 0.0) < it.optDouble("budget", 0.0) })
            add("Smart Spender", "You spent less than budget in a category!", R.drawable.baa)

        if (categories.count { it.optDouble("spent", 0.0) == 0.0 } >= 3)
            add("Thrifty Beginner", "You haven’t spent in 3 categories!", R.drawable.baa)

        if (totalSpent < totalBudget * 0.5)
            add("Economist", "You’ve spent less than 50% of your total budget!", R.drawable.baa)

        if (totalSpent > totalBudget)
            add("Warning Sign", "You've overspent across all categories!", R.drawable.budget)

        if (categories.all { it.optDouble("spent", 0.0) <= it.optDouble("budget", 0.0) && it.optDouble("budget", 0.0) > 0 })
            add("Perfect Planner", "All your spending is within budget!", R.drawable.baa)

        if (categoryCount >= 1 && totalSpent == 0.0)
            add("Budget Setup Complete", "You've created categories but haven't spent yet!", R.drawable.budget)

        return unlocked
    }

    fun backing(view: View) {
        startActivity(Intent(this, dashboard::class.java))
        finish()
    }

    data class Achievement(
        val title: String,
        val description: String,
        val imageResId: Int
    )
}
