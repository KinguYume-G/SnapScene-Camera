package com.example.snapscenecamera

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.snapscenecamera.databinding.ActivityGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private var isMultiSelectMode = false
    
    // 待删除的照片（用于处理权限请求回调）
    private var pendingDeletePhotos: List<Photo> = emptyList()

    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadPhotos()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    // 删除权限请求（Android 10+）
    private val deletePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 用户同意删除
            Log.d(TAG, "Delete permission granted, photos deleted successfully")
            Toast.makeText(
                this,
                "已删除 ${pendingDeletePhotos.size} 张照片",
                Toast.LENGTH_SHORT
            ).show()
            exitMultiSelectMode()
            loadPhotos()
        } else {
            // 用户取消删除
            Log.d(TAG, "Delete permission denied by user")
            Toast.makeText(this, "删除已取消", Toast.LENGTH_SHORT).show()
        }
        pendingDeletePhotos = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: GalleryActivity started")
        
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissionAndLoadPhotos()
    }

    private fun setupUI() {
        // 初始化 Adapter
        adapter = GalleryAdapter(
            onPhotoClick = { photo ->
                if (isMultiSelectMode) {
                    // 多选模式：切换选中状态
                    adapter.toggleSelection(photo)
                    updateBottomBar()
                } else {
                    // 普通模式：查看大图
                    viewPhoto(photo)
                }
            },
            onPhotoLongClick = { photo ->
                // 长按进入多选模式
                if (!isMultiSelectMode) {
                    enterMultiSelectMode()
                    adapter.toggleSelection(photo)
                    updateBottomBar()
                }
            }
        )

        binding.rvPhotos.adapter = adapter

        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 多选按钮
        binding.btnMultiSelect.setOnClickListener {
            if (isMultiSelectMode) {
                exitMultiSelectMode()
            } else {
                enterMultiSelectMode()
            }
        }

        // 全选按钮
        binding.btnSelectAll.setOnClickListener {
            val selectedCount = adapter.getSelectedPhotos().size
            val totalCount = adapter.currentList.size
            
            if (selectedCount == totalCount) {
                adapter.deselectAll()
            } else {
                adapter.selectAll()
            }
            updateBottomBar()
        }

        // 分享按钮
        binding.btnShare.setOnClickListener {
            shareSelectedPhotos()
        }

        // 删除按钮
        binding.btnDelete.setOnClickListener {
            deleteSelectedPhotos()
        }
    }

    private fun checkPermissionAndLoadPhotos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == 
                PackageManager.PERMISSION_GRANTED -> {
                loadPhotos()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadPhotos() {
        Log.d(TAG, "loadPhotos: Starting to load photos from MediaStore")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val photos = mutableListOf<Photo>()
                
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.RELATIVE_PATH
                )

                val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
                } else {
                    "${MediaStore.Images.Media.DATA} LIKE ?"
                }

                val selectionArgs = arrayOf("%SnapScene Camera%")

                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn)
                        val date = it.getLong(dateColumn)

                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        photos.add(Photo(uri, id, name, date))
                    }
                }

                Log.d(TAG, "loadPhotos: Loaded ${photos.size} photos")

                withContext(Dispatchers.Main) {
                    adapter.submitList(photos)
                    updateEmptyView(photos.isEmpty())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPhotos: Failed to load photos", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GalleryActivity, "加载照片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPhotos.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun enterMultiSelectMode() {
        isMultiSelectMode = true
        adapter.isMultiSelectMode = true
        binding.bottomBar.visibility = View.VISIBLE
        binding.btnMultiSelect.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        updateBottomBar()
        Log.d(TAG, "enterMultiSelectMode: Entered multi-select mode")
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        adapter.isMultiSelectMode = false
        adapter.deselectAll()
        binding.bottomBar.visibility = View.GONE
        binding.btnMultiSelect.setImageResource(android.R.drawable.ic_menu_manage)
        Log.d(TAG, "exitMultiSelectMode: Exited multi-select mode")
    }

    private fun updateBottomBar() {
        val selectedCount = adapter.getSelectedPhotos().size
        val totalCount = adapter.currentList.size

        // 更新全选按钮文本
        binding.btnSelectAll.text = if (selectedCount == totalCount && totalCount > 0) {
            "取消全选"
        } else {
            "全选"
        }

        // 更新其他按钮状态
        binding.btnShare.isEnabled = selectedCount > 0
        binding.btnDelete.isEnabled = selectedCount > 0
    }

    private fun viewPhoto(photo: Photo) {
        // Open the photo in EditActivity for full viewing/editing
        val intent = Intent(this, EditActivity::class.java).apply {
            putExtra("image_uri", photo.uri.toString())
        }
        startActivity(intent)
    }

    private fun shareSelectedPhotos() {
        val selectedPhotos = adapter.getSelectedPhotos()
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "请先选择照片", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "shareSelectedPhotos: Sharing ${selectedPhotos.size} photos")

        val uris = ArrayList(selectedPhotos.map { it.uri })
        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "分享照片"))
    }

    private fun deleteSelectedPhotos() {
        val selectedPhotos = adapter.getSelectedPhotos()
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "请先选择照片", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${selectedPhotos.size} 张照片吗？此操作无法撤销。")
            .setPositiveButton("删除") { _, _ ->
                performDelete(selectedPhotos)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performDelete(photos: List<Photo>) {
        Log.d(TAG, "performDelete: Deleting ${photos.size} photos")
        
        pendingDeletePhotos = photos
        val uris = photos.map { it.uri }

        when {
            // Android 11+ (API 30+): 使用 createDeleteRequest
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    deletePermissionLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e(TAG, "performDelete: Failed to create delete request", e)
                    Toast.makeText(this, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    pendingDeletePhotos = emptyList()
                }
            }
            // Android 10 (API 29): 处理 RecoverableSecurityException
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    var deletedCount = 0
                    var pendingException: android.app.RecoverableSecurityException? = null
                    var pendingPhoto: Photo? = null
                    
                    for (photo in photos) {
                        try {
                            val rows = contentResolver.delete(photo.uri, null, null)
                            if (rows > 0) {
                                deletedCount++
                            }
                        } catch (e: SecurityException) {
                            // 尝试转换为 RecoverableSecurityException
                            if (e is android.app.RecoverableSecurityException) {
                                pendingException = e
                                pendingPhoto = photo
                                break
                            } else {
                                Log.e(TAG, "performDelete: SecurityException for ${photo.uri}", e)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "performDelete: Failed to delete ${photo.uri}", e)
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (pendingException != null) {
                            // 需要请求用户权限
                            try {
                                val intentSender = pendingException.userAction.actionIntent.intentSender
                                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                                deletePermissionLauncher.launch(intentSenderRequest)
                            } catch (e: Exception) {
                                Log.e(TAG, "performDelete: Failed to request permission", e)
                                Toast.makeText(this@GalleryActivity, "删除失败: 无法获取权限", Toast.LENGTH_SHORT).show()
                                pendingDeletePhotos = emptyList()
                            }
                        } else if (deletedCount > 0) {
                            Toast.makeText(
                                this@GalleryActivity,
                                "已删除 $deletedCount 张照片",
                                Toast.LENGTH_SHORT
                            ).show()
                            exitMultiSelectMode()
                            loadPhotos()
                            pendingDeletePhotos = emptyList()
                        } else {
                            Toast.makeText(this@GalleryActivity, "没有照片被删除", Toast.LENGTH_SHORT).show()
                            pendingDeletePhotos = emptyList()
                        }
                    }
                }
            }
            // Android 9及以下 (API 28-): 直接删除
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        var deletedCount = 0
                        photos.forEach { photo ->
                            val rows = contentResolver.delete(photo.uri, null, null)
                            if (rows > 0) {
                                deletedCount++
                            }
                        }

                        Log.d(TAG, "performDelete: Deleted $deletedCount photos")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@GalleryActivity,
                                "已删除 $deletedCount 张照片",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            exitMultiSelectMode()
                            loadPhotos()
                            pendingDeletePhotos = emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "performDelete: Failed to delete photos", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@GalleryActivity,
                                "删除失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            pendingDeletePhotos = emptyList()
                        }
                    }
                }
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("查看作品需要访问您的照片。请在设置中授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                // Jump to app settings
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onBackPressed() {
        if (isMultiSelectMode) {
            exitMultiSelectMode()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "GalleryActivity"
    }
}
