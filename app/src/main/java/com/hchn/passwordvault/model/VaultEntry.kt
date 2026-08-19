package com.hchn.passwordvault.model

import org.json.JSONObject
import java.util.UUID

data class VaultEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val website: String = "",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("username", username)
        put("password", password)
        put("website", website)
        put("note", note)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject) = VaultEntry(
            id = json.getString("id"),
            title = json.getString("title"),
            username = json.optString("username"),
            password = json.getString("password"),
            website = json.optString("website"),
            note = json.optString("note"),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

