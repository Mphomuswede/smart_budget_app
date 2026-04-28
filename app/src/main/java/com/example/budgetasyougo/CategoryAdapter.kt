package com.example.budgetasyougo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<spendings.CategoryData>,
    private val onItemClick: (spendings.CategoryData) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.categoryTitle)
        val budget = itemView.findViewById<TextView>(R.id.categoryBudget)
        val spent = itemView.findViewById<TextView>(R.id.categorySpent)

        fun bind(data: spendings.CategoryData) {
            title.text = data.title
            budget.text = "Budget: R %.2f".format(data.budget)
            spent.text = "Spent: R %.2f".format(data.spent)
            itemView.setOnClickListener { onItemClick(data) }

            // Slide-in animation
            val animation = AnimationUtils.loadAnimation(itemView.context, android.R.anim.slide_in_left)
            itemView.startAnimation(animation)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }
}
