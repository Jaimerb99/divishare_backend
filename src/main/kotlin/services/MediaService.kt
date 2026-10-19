package com.jrb.services

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaService {

    private val tag = "MediaService"

    private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", Constants.CloudinaryConfig.CLOUD_NAME,
            "api_key", Constants.CloudinaryConfig.API_KEY,
            "api_secret", Constants.CloudinaryConfig.API_SECRET
        )
    )

    /**
     * Uploads a file to Cloudinary and returns its secure public URL.
     *
     * @param fileBytes The raw file content in bytes.
     * @param folder Destination folder.
     * @param publicId The unique identifier for the file (e.g., expenseId).
     * @return The secure HTTPS URL of the uploaded image, or null if the upload fails.
     */
    suspend fun uploadImage(fileBytes: ByteArray, folder: String, publicId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val params = ObjectUtils.asMap(
                    "folder", "divishare/$folder",
                    "public_id", publicId,
                    "overwrite", true, // Ensures it overwrites the previous receipt if updated
                    "resource_type", "auto"
                )

                val uploadResult = cloudinary.uploader().upload(fileBytes, params)

                uploadResult["secure_url"] as String
            } catch (e: Exception) {
                AppLogger.error(tag, "Error uploading image to Cloudinary", e)
                null
            }
        }
    }

    /**
     * Generates the secure public download URL for a Cloudinary resource based on its public ID and folder.
     */
    fun getFileUrl(publicId: String, folder: String): String {
        return cloudinary.url().secure(true).generate("divishare/$folder/$publicId")
    }


    /**
     * Generates the secure public download URL for a Cloudinary resource based on its public ID.
     */
    fun getReceiptUrl(publicId: String, folder: String = Constants.CloudinaryConfig.FOLDER_RECEIPTS): String {
        // The SDK automatically constructs the secure HTTPS URL
        return cloudinary.url().secure(true).generate("divishare/$folder/$publicId")
    }
}