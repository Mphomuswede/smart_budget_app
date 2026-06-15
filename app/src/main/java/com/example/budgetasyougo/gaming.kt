package com.example.budgetasyougo

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
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaming)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""
        prefs = getSharedPreferences("budgetAppPrefs", MODE_PRIVATE)
        expensePrefs = getSharedPreferences("StructuredExpenses", MODE_PRIVATE)

        recyclerView = findViewById(R.id.achievementRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val categories = loadCategories()
        val expenses = loadExpenses()

        val achievements = getAllAchievements(categories, expenses)
        adapter = AchievementAdapter(achievements)
        recyclerView.adapter = adapter
    }

    private fun loadCategories(): List<JSONObject> {
        val jsonStr = prefs.getString("categories_$userEmail", "[]") ?: "[]"
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

    private fun getAllAchievements(categories: List<JSONObject>, expenses: List<JSONObject>): List<Achievement> {
        val allPossible = mutableListOf<Achievement>()

        val categoryCount = categories.size
        val totalBudget = categories.sumOf { it.optDouble("budget", 0.0) }
        val totalSpent = categories.sumOf { it.optDouble("spent", 0.0) }
        val expenseCount = expenses.size
        val uniqueDays = expenses.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.optLong("date", 0)
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size

        // Define all possible achievements and check if they are unlocked
        allPossible.add(Achievement("First Step", "Create your first category!", R.drawable.baa, categoryCount >= 1))
        allPossible.add(Achievement("Budget Boss", "Manage 5+ categories!", R.drawable.baa, categoryCount >= 5))
        allPossible.add(Achievement("Logged In", "Log your first expense!", R.drawable.ic_coin, expenseCount >= 1))
        allPossible.add(Achievement("Habit Builder", "Log expenses on 3 different days!", R.drawable.ic_coin, uniqueDays >= 3))
        allPossible.add(Achievement("Consistency King", "Log across a full week!", R.drawable.ic_coin, uniqueDays >= 7))
        
        val inGoal = categories.count { 
            val s = it.optDouble("spent", 0.0)
            s >= it.optDouble("minGoal", 0.0) && s <= it.optDouble("budget", 0.0) && s > 0
        }
        allPossible.add(Achievement("Goal Getter", "Stay within goals in a category!", R.drawable.baa, inGoal >= 1))
        allPossible.add(Achievement("Smart Saver", "Stay under total budget!", R.drawable.baa, totalSpent < totalBudget && totalSpent > 0))

        return allPossible
    }

    fun backing(view: View) {
        finish() // Returns to Dashboard
    }

    data class Achievement(
        val title: String,
        val description: String,
        val imageResId: Int,
        val isUnlocked: Boolean


    )
}
