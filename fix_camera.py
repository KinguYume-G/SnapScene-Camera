#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

file_path = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\CameraActivity.kt'

# 读取文件
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 替换onImageSaved方法
old_onImageSaved = r'''override fun onImageSaved\(output: ImageCapture\.OutputFileResults\) \{
                val savedUri = Uri\.fromFile\(photoFile\)
                val msg = "Photo capture succeeded: \$savedUri"
                Log\.d\(TAG, msg\)
                
                // 跳转到 EditActivity
                val intent = Intent\(baseContext, EditActivity::class\.java\)
                intent\.putExtra\("image_uri", savedUri\.toString\(\)\)
                startActivity\(intent\)
                finish\(\)
            \}'''

new_onImageSaved = '''override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
                
                // 在协程中保存到MediaStore
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 保存到MediaStore并获取URI
                        val mediaStoreUri = saveToMediaStore(photoFile)
                        
                        Log.d(TAG, "Image saved to MediaStore: $mediaStoreUri")
                        
                        // 切换回主线程启动EditActivity
                        withContext(Dispatchers.Main) {
                            val intent = Intent(baseContext, EditActivity::class.java)
                            intent.putExtra("image_uri", savedUri.toString())
                            if (mediaStoreUri != null) {
                                intent.putExtra("original_uri", mediaStoreUri.toString())
                            }
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save to MediaStore", e)
                        // 即使保存失败，仍然继续编辑流程
                        withContext(Dispatchers.Main) {
                            val intent = Intent(baseContext, EditActivity::class.java)
                            intent.putExtra("image_uri", savedUri.toString())
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }'''

content = re.sub(old_onImageSaved, new_onImageSaved, content, flags=re.DOTALL)

# 2. 在onDestroy之前添加saveToMediaStore方法
saveToMediaStore_method = '''
    
    private suspend fun saveToMediaStore(file: File): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // 准备元数据
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/SnapScene Camera")
                    // Android 10+ 使用IS_PENDING机制
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                // 插入到MediaStore
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                if (uri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry")
                    return@withContext null
                }
                
                // 写入文件内容
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // 标记为完成
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                Log.d(TAG, "Successfully saved to MediaStore: $uri")
                uri
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to MediaStore", e)
                null
            }
        }
    }
'''

# 在onDestroy之前插入
content = content.replace('    override fun onDestroy() {', saveToMediaStore_method + '    override fun onDestroy() {')

# 写回文件
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('✅ Successfully modified CameraActivity.kt')
print('✅ Updated onImageSaved method')
print('✅ Added saveToMediaStore method')
