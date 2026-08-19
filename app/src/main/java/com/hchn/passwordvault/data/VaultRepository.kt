package com.hchn.passwordvault.data

import android.content.Context
import com.hchn.passwordvault.model.VaultEntry
import org.json.JSONArray
import javax.crypto.AEADBadTagException
import javax.crypto.SecretKey

class VaultRepository(context: Context) {
    private val preferences = context.getSharedPreferences("encrypted_vault", Context.MODE_PRIVATE)
    private var sessionKey: SecretKey? = null

    val isConfigured: Boolean
        get() = preferences.contains(KEY_SALT) && preferences.contains(KEY_PAYLOAD)

    fun create(masterPassword: CharArray): List<VaultEntry> {
        require(masterPassword.size >= 8) { "主密码至少需要 8 位" }
        val salt = CryptoManager.newSalt()
        val key = CryptoManager.deriveKey(masterPassword, salt)
        val payload = CryptoManager.encrypt("[]", key)
        check(preferences.edit()
            .putString(KEY_SALT, CryptoManager.encode(salt))
            .putString(KEY_PAYLOAD, payload)
            .commit()) { "无法保存密码库" }
        sessionKey = key
        return emptyList()
    }

    fun unlock(masterPassword: CharArray): List<VaultEntry> {
        val saltValue = preferences.getString(KEY_SALT, null) ?: error("密码库尚未初始化")
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: error("密码库数据缺失")
        val key = CryptoManager.deriveKey(masterPassword, CryptoManager.decode(saltValue))
        return try {
            decodeEntries(CryptoManager.decrypt(payload, key)).also { sessionKey = key }
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("主密码不正确")
        } catch (_: SecurityException) {
            throw IllegalArgumentException("主密码不正确")
        }
    }

    fun save(entries: List<VaultEntry>) {
        val key = sessionKey ?: error("密码库已锁定")
        val array = JSONArray().apply { entries.forEach { put(it.toJson()) } }
        val payload = CryptoManager.encrypt(array.toString(), key)
        check(preferences.edit().putString(KEY_PAYLOAD, payload).commit()) { "无法保存密码库" }
    }

    fun lock() {
        sessionKey = null
    }

    private fun decodeEntries(json: String): List<VaultEntry> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                add(VaultEntry.fromJson(array.getJSONObject(index)))
            }
        }
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_PAYLOAD = "payload"
    }
}

