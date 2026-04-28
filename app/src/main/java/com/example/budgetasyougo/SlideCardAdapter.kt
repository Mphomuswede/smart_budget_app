package com.example.budgetasyougo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SlideCardAdapter(private val items: List<dashboard.CardInfo>) :
    RecyclerView.Adapter<SlideCardAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
       // val bgImage: ImageView = view.findViewById(R.id.bgImage)
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slide_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]
      //  holder.bgImage.setImageResource(item.backgroundResId)
        holder.iconImage.setImageResource(item.iconResId)
        holder.titleText.text = item.title
        holder.descriptionText.text = item.description
    }

    override fun getItemCount(): Int = items.size
}
