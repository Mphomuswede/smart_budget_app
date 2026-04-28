package com.example.budgetasyougo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class viewing_categories : AppCompatActivity() {

    private lateinit var adapter: CategoryAdapters
    private lateinit var categoryList: MutableList<JSONObject>
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var userEmail: String
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var categoryKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewing_categories)

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: "non"
        prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        categoryKey = "categories_$userEmail"

        recyclerView = findViewById(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        categoryList = loadCategories()
        adapter = CategoryAdapters(categoryList, { categoryId ->
            deleteCategory(categoryId)
        }, this, prefs,userEmail)

        recyclerView.adapter = adapter

        searchEditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCategories(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            val intent = Intent(this, dashboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadCategories(): MutableList<JSONObject> {
        val json = prefs.getString(categoryKey, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }
        return list
    }

    private fun deleteCategory(id: String) {
        val newArray = JSONArray()
        for (item in categoryList) {
            if (item.getString("id") != id) {
                newArray.put(item)
            }
        }
        prefs.edit().putString(categoryKey, newArray.toString()).apply()
        categoryList = loadCategories()
        adapter.updateList(categoryList)
    }

    private fun filterCategories(query: String) {
        val filtered = categoryList.filter {
            it.optString("name").contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered.toMutableList())
    }
}
