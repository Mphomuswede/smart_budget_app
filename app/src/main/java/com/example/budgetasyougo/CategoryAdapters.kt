package com.example.budgetasyougo

import android.content.Context
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class CategoryAdapters(
    private var categoryList: MutableList<JSONObject>,
    private val onDeleteClick: (String) -> Unit,
    private val context: Context,
    private val prefs: android.content.SharedPreferences,
    private val email:String
) : RecyclerView.Adapter<CategoryAdapters.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.categoryName)
        val desc: TextView = view.findViewById(R.id.categoryDesc)
        val info: TextView = view.findViewById(R.id.categoryInfo)
        val date: TextView = view.findViewById(R.id.categoryDate)
        val deleteBtn: Button = view.findViewById(R.id.deleteButton)
        val viewBtn: Button = view.findViewById(R.id.viewButton)
        val spentInfo: TextView = view.findViewById(R.id.spentInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categoryList[position]
        val df = DecimalFormat("#,##0.00")

        holder.name.text = item.optString("name")
        holder.desc.text = item.optString("description")
        holder.info.text = "Budget: R ${df.format(item.optDouble("budget"))}"
        holder.date.text = "Created: ${item.optString("createdAt")}"
        holder.spentInfo.text = " Spent: R " +df.format(item.optDouble("spent"))



        holder.deleteBtn.setOnClickListener {
            val id = item.optString("id")
            if (id.isNotEmpty()) {
                onDeleteClick(id)
            }
        }

        holder.viewBtn.setOnClickListener {
            val categoryName = item.optString("name")
            val expensePrefs = context.getSharedPreferences("Expenses", Context.MODE_PRIVATE)
            val allExpensesString = expensePrefs.getString(email, "") ?: ""

            if (allExpensesString.isBlank()) {
                Toast.makeText(context, "No expenses saved yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val allEntries = allExpensesString.split("\n\n")
            val matchedExpenses = allEntries.filter { it.contains("Category: $categoryName") }

            if (matchedExpenses.isEmpty()) {
                Toast.makeText(context, "No expenses found for this category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_expense_list, null)
            val container = dialogView.findViewById<LinearLayout>(R.id.expenseContainer)

            // Inflate each expense into the container
            matchedExpenses.forEachIndexed { index, entry ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_expense, container, false)
                val expenseText = itemView.findViewById<TextView>(R.id.expenseText)
                val expenseImage = itemView.findViewById<ImageView>(R.id.expenseImage)

                // Show expense text
                expenseText.text = "Expense ${index + 1}:\n$entry"

                // Parse image path from entry string ( "ImagePath: <path>")
                val imagePathLine = entry.lines().find { it.startsWith("ImagePath:") }
                val imagePath = imagePathLine?.substringAfter("ImagePath:")?.trim()

                if (!imagePath.isNullOrEmpty()) {
                    // Load image if exists
                    val imgFile = java.io.File(imagePath)
                    if (imgFile.exists()) {
                        expenseImage.setImageURI(android.net.Uri.fromFile(imgFile))
                        expenseImage.visibility = View.VISIBLE
                    }
                }

                container.addView(itemView)
            }

            val dialog = android.app.AlertDialog.Builder(context)
                .setTitle("Expenses for $categoryName")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()

            dialog.show()

            // Only these 2 lines added for styling
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#81C784"))
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

    }

    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: MutableList<JSONObject>) {
        categoryList = newList
        notifyDataSetChanged()
    }
}
