package com.example.snapscenecamera

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

sealed class BackgroundItem {
    data class ColorItem(val color: Int) : BackgroundItem()
    data class ImageItem(val drawableId: Int) : BackgroundItem()
    object Original : BackgroundItem()
}

class BackgroundAdapter(
    private val items: List<BackgroundItem>,
    private val onItemSelected: (BackgroundItem) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.ViewHolder>() {
    
    private var selectedPosition = 0
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorCircle: View = view.findViewById(R.id.colorCircle)
        val selectionBorder: View = view.findViewById(R.id.selectionBorder)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        when (item) {
            is BackgroundItem.Original -> {
                holder.colorCircle.setBackgroundResource(R.drawable.ic_original_image)
            }
            is BackgroundItem.ColorItem -> {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(item.color)
                holder.colorCircle.background = drawable
            }
            is BackgroundItem.ImageItem -> {
                holder.colorCircle.setBackgroundResource(item.drawableId)
            }
        }
        
        // 选中状态
        holder.selectionBorder.visibility = if (position == selectedPosition) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        
        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onItemSelected(item)
        }
    }
    
    override fun getItemCount() = items.size
}
