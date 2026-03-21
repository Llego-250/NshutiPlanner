package com.example.nshutiplanner.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CloudinaryConfig {
    const val CLOUD_NAME = "dn4vlox7r"
    const val API_KEY    = "554832713174717"
    const val API_SECRET = "S2Ka3w7VykbDfzjQnzQIFQmixlY"
    const val UPLOAD_PRESET = "nshuti_unsigned"
}

class CloudinaryRepository(private val context: Context) {

    private val client = OkHttpClient()

    /** Upload any image/video URI to Cloudinary. Returns the secure URL. */
    suspend fun upload(uri: Uri, folder: String = "nshuti"): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: error("Cannot read file")

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val isVideo = mimeType.startsWith("video")
        val resourceType = if (isVideo) "video" else "image"

        val url = "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/$resourceType/upload"

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "upload",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
            .addFormDataPart("folder", folder)
            .build()

        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: error("Empty response"))

        if (!response.isSuccessful) {
            error(json.optJSONObject("error")?.optString("message") ?: "Upload failed")
        }

        json.getString("secure_url")
    }
}
