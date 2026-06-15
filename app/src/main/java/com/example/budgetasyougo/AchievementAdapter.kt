package com.example.budgetasyougo

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementAdapter(private val items: List<gaming.Achievement>) :
    RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badgeImage: ImageView = view.findViewById(R.id.badgeImage)
        val title: TextView = view.findViewById(R.id.title)
        val description: TextView = view.findViewById(R.id.description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.description.text = item.description
        holder.badgeImage.setImageResource(item.imageResId)

        if (item.isUnlocked) {
            // Achievement earned: Full color
            holder.badgeImage.clearColorFilter()
            holder.badgeImage.alpha = 1.0f
            holder.title.setTextColor(Color.WHITE)
        } else {
            // Achievement locked: Grey out and fade
            val matrix = ColorMatrix()
            matrix.setSaturation(0f) // Remove color
            holder.badgeImage.colorFilter = ColorMatrixColorFilter(matrix)
            holder.badgeImage.alpha = 0.4f
            holder.title.setTextColor(Color.GRAY)
        }
    }
}
