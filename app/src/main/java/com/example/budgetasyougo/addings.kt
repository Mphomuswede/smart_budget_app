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
        val backButton = findViewById<ImageView>(R.id.backButton)

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

            if (desc.isEmpty()) {
                description.error = "Enter description"
                isValid = false
            }

            if (minStr.isEmpty()) {
                minAmount.error = "Enter min amount"
                isValid = false
            }

            if (maxStr.isEmpty()) {
                maxAmount.error = "Enter max amount"
                isValid = false
            }

            val min = minStr.toDoubleOrNull()
            val max = maxStr.toDoubleOrNull()

            if (min == null || min < 0) {
                minAmount.error = "Invalid min amount"
                isValid = false
            }

            if (max == null || max < 0) {
                maxAmount.error = "Invalid max amount"
                isValid = false
            }

            val totalSum = (min ?: 0.0) + (max ?: 0.0)

            if (!isValid) return@setOnClickListener

            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val userEmail = sharedPref.getString("email", "") ?: "non"
            val prefs = getSharedPreferences("budgetAppPrefs", Context.MODE_PRIVATE)
            val categoryKey = "categories_$userEmail"
            val existingCategoriesJson = prefs.getString(categoryKey, "[]")
            val categoryArray = JSONArray(existingCategoriesJson)

            var categoryFound = false

            for (i in 0 until categoryArray.length()) {
                val obj = categoryArray.getJSONObject(i)
                if (obj.optString("name") == selectedCategory) {
                    val budget = obj.optDouble("budget", 0.0)
                    val spent = obj.optDouble("spent", 0.0)

                    if (spent + totalSum > budget) {
                        //Snackbar.make(rootLayout, ""+(spent+totalSum)+" and "+budget, Snackbar.LENGTH_LONG).show()

                        Snackbar.make(rootLayout, "Not enough budget left for this category", Snackbar.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    obj.put("spent", spent + totalSum)
                    categoryFound = true
                    break
                }
            }

            if (!categoryFound) {
                Snackbar.make(rootLayout, "Category not found in preferences", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString(categoryKey, categoryArray.toString()).apply()

            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = """
        Category: $selectedCategory
        Name: $name
        Description: $desc
        Min: ${minStr}
        Max: ${maxStr}
        Total: R ${"%.2f".format(totalSum)}
        DateTime: $dateTime
        ImagePath: $imagePath
    """.trimIndent()

            val expensePrefs = getSharedPreferences("Expenses", Context.MODE_PRIVATE)
            val old = expensePrefs.getString(userEmail, "") ?: ""
            expensePrefs.edit().putString(userEmail, old + "\n\n" + entry).apply()

            Snackbar.make(rootLayout, "Expense saved", Snackbar.LENGTH_SHORT).show()





            val pieChart = findViewById<PieChart>(R.id.categoryPieChart)
            pieChart.visibility = View.VISIBLE

            val entries = mutableListOf<PieEntry>()

// Add entries
            entries.add(PieEntry(totalSum.toFloat(), "Total"))
            entries.add(PieEntry((min ?: 0.0).toFloat(), "Min"))
            entries.add(PieEntry((max ?: 0.0).toFloat(), "Max"))

            var categoryBudget = 0.0
            var categorySpent = 0.0

            for (i in 0 until categoryArray.length()) {
                val obj = categoryArray.getJSONObject(i)
                if (obj.optString("name") == selectedCategory) {
                    categoryBudget = obj.optDouble("budget", 0.0)
                    categorySpent = obj.optDouble("spent", 0.0)
                    break
                }
            }

            entries.add(PieEntry(categorySpent.toFloat(), "Spent"))
            entries.add(PieEntry(categoryBudget.toFloat(), "Budget"))

            val dataSet = PieDataSet(entries, "Expense Breakdown")

// Color palette for each entry
            dataSet.colors = listOf(
                Color.parseColor("#F44336"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FF9800")
            )

            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            dataSet.sliceSpace = 4f

            val pieData = PieData(dataSet)
            pieChart.data = pieData

            pieChart.description.isEnabled = false
            pieChart.setUsePercentValues(false)
            pieChart.setEntryLabelColor(Color.BLACK)
            pieChart.setEntryLabelTextSize(12f)
            pieChart.setExtraOffsets(10f, 10f, 10f, 10f)
            pieChart.animateY(1000)
            pieChart.invalidate()


            // Clear input fields after saving
            expenseName.text.clear()
            description.text.clear()
            minAmount.text.clear()
            maxAmount.text.clear()
            photoPreview.setImageDrawable(null)
            photoPreview.visibility = View.GONE
            imagePath = ""

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

                val photoPreview = findViewById<ImageView>(R.id.photoPreview)
                photoPreview.setImageBitmap(photo)
                photoPreview.visibility = ImageView.VISIBLE
            }
        }
    }

    fun backfrom(view: View) {

        val intent = Intent(this, dashboard::class.java)
        startActivity(intent)
        finish()

    }
}
