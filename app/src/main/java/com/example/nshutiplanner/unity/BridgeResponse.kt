package com.example.nshutiplanner.unity

import org.json.JSONObject

data class BridgeResponse(
    val success: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val displayName: String = "",
    val error: String = ""
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("success", success)
            put("latitude", latitude)
            put("longitude", longitude)
            put("displayName", displayName)
            put("error", error)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): BridgeResponse {
            val obj = JSONObject(json)
            return BridgeResponse(
                success = obj.optBoolean("success", false),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                displayName = obj.optString("displayName", ""),
                error = obj.optString("error", "")
            )
        }
    }
}
