package com.jrb.models

import com.jrb.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UploadReceiptImageRequest(
    val expenseId: String,
    val base64Image: String,
    val folder: String = Constants.CloudinaryConfig.FOLDER_RECEIPTS
)

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String? = null
)

@Serializable
data class UploadAvatarRequest(
    val base64Image: String
)