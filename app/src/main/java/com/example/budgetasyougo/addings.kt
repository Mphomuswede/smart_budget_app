package com.example.budgetasyougo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Color


class addings : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 101
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addings)

        val expenseName = findViewById<EditText>(R.id.expenseName)
        val description = findViewById<EditText>(R.id.description)
        val minAmount = findViewById<EditText>(R.id.minAmount)
        val maxAmount = findViewById<EditText>(R.id.maxAmount)
        val takePhotoButton = findViewById<Button>(R.id.takePhotoButton)
        val photoPreview = findViewById<ImageView>(R.id.photoPreview)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("email", "") ?: "non"

        val categoryKey = "categories_$userEmail"
        val existingCategoriesJson = prefs.getString(categoryKey, "[]")
        val categoryArray = JSONArray(existingCategoriesJson)

        val categoryNames = mutableListOf<String>()
        for (i in 0 until categoryArray.length()) {
            val obj = categoryArray.getJSONObject(i)
            val name = obj.optString("name", "")
            if (name.isNotEmpty()) {
                categoryNames.add(name)
            }
        }
        val spinner = findViewById<Spinner>(R.id.categorySpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        spinner.adapter = adapter

        takePhotoButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }


        saveButton.setOnClickListener {
            val rootLayout = findViewById<View>(android.R.id.content)

            val selectedCategory = spinner?.selectedItem?.toString() ?: ""
            val name = expenseName.text.toString().trim()
            val desc = description.text.toString().trim()
            val minStr = minAmount.text.toString().trim()
            val maxStr = maxAmount.text.toString().trim()
            var isValid = true

            if (selectedCategory.isEmpty()) {
                Snackbar.make(rootLayout, "Please select a category", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                expenseName.error = "Enter expense name"
                isValid = false
            }

            if (minStr.isEmpty()) {
                minAmount.error = "Enter amount"
                isValid = false
            }

            val totalSum = minStr.toDoubleOrNull() ?: 0.0

            if (!isValid) return@setOnClickListener

            // Update category spent
            for (i in 0 until categoryArray.length()) {
                val obj = categoryArray.getJSONObject(i)
                if (obj.optString("name") == selectedCategory) {
                    val budget = obj.optDouble("budget", 0.0)
                    val spent = obj.optDouble("spent", 0.0)

                    if (spent + totalSum > budget) {
                        Snackbar.make(rootLayout, "Category budget exceeded!", Snackbar.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    obj.put("spent", spent + totalSum)
                    break
                }
            }
            prefs.edit().putString(categoryKey, categoryArray.toString()).apply()

            // Save structured expense
            val expensePrefs = getSharedPreferences("StructuredExpenses", Context.MODE_PRIVATE)
            val expensesJson = expensePrefs.getString(userEmail, "[]")
            val expensesArray = JSONArray(expensesJson)

            val newExpense = JSONObject().apply {
                put("category", selectedCategory)
                put("name", name)
                put("amount", totalSum)
                put("date", System.currentTimeMillis())
                put("imagePath", imagePath ?: "")
            }
            expensesArray.put(newExpense)
            expensePrefs.edit().putString(userEmail, expensesArray.toString()).apply()

            Snackbar.make(rootLayout, "Expense saved", Snackbar.LENGTH_SHORT).show()

            // Reset UI
            expenseName.text.clear()
            minAmount.text.clear()
            description.text.clear()
            photoPreview.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            if (photo != null) {
                val file = File(filesDir, "IMG_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { photo.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                imagePath = file.absolutePath
                findViewById<ImageView>(R.id.photoPreview).apply {
                    setImageBitmap(photo)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    fun backfrom(view: View) {
        startActivity(Intent(this, dashboard::class.java))
        finish()
    }
}
