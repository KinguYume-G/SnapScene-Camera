package com.example.snapscenecamera

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapscenecamera.databinding.ItemGalleryBinding

data class Photo(
    val uri: Uri,
    val id: Long,
    val displayName: String,
    val dateAdded: Long,
    var isSelected: Boolean = false
)

class GalleryAdapter(
    private val onPhotoClick: (Photo) -> Unit,
    private val onPhotoLongClick: (Photo) -> Unit
) : ListAdapter<Photo, GalleryAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    var isMultiSelectMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class PhotoViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            // 使用 Glide 加载缩略图
            Glide.with(binding.ivThumbnail.context)
                .load(photo.uri)
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(binding.ivThumbnail)

            // 显示选中状态
            if (isMultiSelectMode) {
                binding.vMask.visibility = if (photo.isSelected) View.VISIBLE else View.GONE
                binding.ivCheck.visibility = if (photo.isSelected) View.VISIBLE else View.GONE
            } else {
                binding.vMask.visibility = View.GONE
                binding.ivCheck.visibility = View.GONE
            }

            // 点击事件
            binding.root.setOnClickListener {
                onPhotoClick(photo)
            }

            // 长按事件
            binding.root.setOnLongClickListener {
                onPhotoLongClick(photo)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedPhotos(): List<Photo> {
        return currentList.filter { it.isSelected }
    }

    fun selectAll() {
        currentList.forEach { it.isSelected = true }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        currentList.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun toggleSelection(photo: Photo) {
        photo.isSelected = !photo.isSelected
        notifyItemChanged(currentList.indexOf(photo))
    }
}

class PhotoDiffCallback : DiffUtil.ItemCallback<Photo>() {
    override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean {
        return oldItem == newItem
    }
}
