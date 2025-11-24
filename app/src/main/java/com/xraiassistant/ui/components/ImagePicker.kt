package com.xraiassistant.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.AIImageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Image Picker Button with Preview
 *
 * Equivalent to iOS PhotosPicker functionality
 * Allows selecting up to 5 images and displays them in a horizontal row
 */
@Composable
fun ImagePickerButton(
    selectedImages: List<AIImageContent>,
    onImagesSelected: (List<AIImageContent>) -> Unit,
    maxImages: Int = 5,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Load images in background
            scope.launch {
                val images = loadImagesFromUris(context, uris)
                onImagesSelected(selectedImages + images)
            }
        }
    }

    // Image picker button
    IconButton(
        onClick = {
            // Calculate remaining slots
            val remainingSlots = maxImages - selectedImages.size
            if (remainingSlots > 0) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        },
        enabled = selectedImages.size < maxImages,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = "Add images",
            tint = if (selectedImages.size < maxImages) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
    }
}

/**
 * Image Preview Row - Shows selected images with remove buttons
 *
 * Equivalent to iOS image preview in chat input
 */
@Composable
fun ImagePreviewRow(
    images: List<AIImageContent>,
    onRemoveImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(images) { index, imageContent ->
            ImagePreviewItem(
                imageContent = imageContent,
                onRemove = { onRemoveImage(index) }
            )
        }
    }
}

/**
 * Single Image Preview Item with Remove Button
 */
@Composable
private fun ImagePreviewItem(
    imageContent: AIImageContent,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Image
        val bitmap = remember(imageContent) {
            BitmapFactory.decodeByteArray(imageContent.data, 0, imageContent.data.size)
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Selected image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Remove button overlay
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .padding(2.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // File size indicator
        val sizeKB = imageContent.data.size / 1024
        Text(
            text = "${sizeKB}KB",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Load images from URIs and convert to AIImageContent
 */
private suspend fun loadImagesFromUris(
    context: android.content.Context,
    uris: List<Uri>
): List<AIImageContent> = withContext(Dispatchers.IO) {
    uris.mapNotNull { uri ->
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Load bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // Resize if too large (max 2048px on longest side)
                val resizedBitmap = resizeBitmapIfNeeded(bitmap, maxSize = 2048)

                // Compress to JPEG
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val imageData = outputStream.toByteArray()

                // Get filename
                val filename = uri.lastPathSegment ?: "image.jpg"

                AIImageContent(
                    data = imageData,
                    mimeType = "image/jpeg",
                    filename = filename
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Resize bitmap if it exceeds max size
 */
private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    // Check if resizing is needed
    if (width <= maxSize && height <= maxSize) {
        return bitmap
    }

    // Calculate new dimensions
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
        newWidth = maxSize
        newHeight = (maxSize / aspectRatio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (maxSize * aspectRatio).toInt()
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

/**
 * Image Attachment Info Display
 * Shows count and total size of attachments
 */
@Composable
fun ImageAttachmentInfo(
    images: List<AIImageContent>,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    val totalSizeKB = images.sumOf { it.data.size } / 1024

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${images.size} image${if (images.size > 1) "s" else ""} (${totalSizeKB}KB)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
