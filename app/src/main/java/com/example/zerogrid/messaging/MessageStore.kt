package com.example.zerogrid.messaging

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent message store for ZeroGrid DM conversations.
 * Keyed by peer Node ID. Backed by SharedPreferences as JSON arrays.
 * Messages survive app restarts and mesh reconnections.
 */
class MessageStore private constructor(context: Context) {

    companion object {
        private const val TAG = "MessageStore"
        private const val PREFS_NAME = "zerogrid_messages"
        private const val CONV_KEY_PREFIX = "conv_"
        private const val MAX_MESSAGES_PER_PEER = 200

        @Volatile
        private var INSTANCE: MessageStore? = null

        fun getInstance(context: Context): MessageStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Load all stored messages for a specific peer. */
    fun getConversation(peerId: String): List<StoredMessage> {
        return try {
            val json = prefs.getString("$CONV_KEY_PREFIX$peerId", null) ?: return emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    val isMine = obj.optBoolean("isMine", false)
                    val statusStr = if (obj.has("status")) obj.optString("status") else null
                    val status = if (!statusStr.isNullOrEmpty()) {
                        try {
                            MessageStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            if (isMine) MessageStatus.SENT else MessageStatus.DELIVERED
                        }
                    } else {
                        if (isMine) MessageStatus.SENT else MessageStatus.DELIVERED
                    }
                    StoredMessage(
                        id = obj.optString("id"),
                        senderId = obj.optString("senderId"),
                        recipientId = obj.optString("recipientId"),
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        hopCount = obj.optInt("hopCount", 0),
                        isMine = isMine,
                        status = status
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading conversation for $peerId", e)
            emptyList()
        }
    }

    /** Append a new message to a peer's conversation, persisting to disk. */
    fun appendMessage(peerId: String, msg: StoredMessage) {
        try {
            val current = getConversation(peerId).toMutableList()
            // Avoid exact duplicate (same id)
            if (current.any { it.id == msg.id }) return
            current.add(msg)
            // Prune oldest if over limit
            val pruned = if (current.size > MAX_MESSAGES_PER_PEER) {
                current.takeLast(MAX_MESSAGES_PER_PEER)
            } else current
            val arr = JSONArray()
            pruned.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("senderId", m.senderId)
                    put("recipientId", m.recipientId)
                    put("text", m.text)
                    put("timestamp", m.timestamp)
                    put("hopCount", m.hopCount)
                    put("isMine", m.isMine)
                    put("status", m.status.name)
                })
            }
            prefs.edit().putString("$CONV_KEY_PREFIX$peerId", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error appending message for $peerId", e)
        }
    }

    /** Update status of a specific message on disk. */
    fun updateMessageStatus(peerId: String, messageId: String, newStatus: MessageStatus): Boolean {
        return try {
            val current = getConversation(peerId).toMutableList()
            val index = current.indexOfFirst { it.id == messageId }
            if (index == -1) return false
            val old = current[index]
            current[index] = old.copy(status = newStatus)
            val arr = JSONArray()
            current.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("senderId", m.senderId)
                    put("recipientId", m.recipientId)
                    put("text", m.text)
                    put("timestamp", m.timestamp)
                    put("hopCount", m.hopCount)
                    put("isMine", m.isMine)
                    put("status", m.status.name)
                })
            }
            prefs.edit().putString("$CONV_KEY_PREFIX$peerId", arr.toString()).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status for message $messageId for $peerId", e)
            false
        }
    }

    /** Permanently deletes a peer conversation and associated alias from local storage. */
    fun deleteConversation(peerId: String) {
        try {
            prefs.edit().remove("$CONV_KEY_PREFIX$peerId").remove("alias_$peerId").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation for $peerId", e)
        }
    }

    /** Wipes all stored conversations and cached messages from local storage. */
    fun clearAllMessages() {
        try {
            val editor = prefs.edit()
            prefs.all.keys
                .filter { it.startsWith(CONV_KEY_PREFIX) }
                .forEach { editor.remove(it) }
            editor.apply()
            Log.d(TAG, "All conversation messages cleared from disk.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all conversation messages", e)
        }
    }

    /**
     * Load all peer IDs that have at least one stored message.
     * Returns them ordered by the timestamp of the most recent message (newest first).
     */
    fun getAllConversationPeerIds(): List<String> {
        return try {
            prefs.all.keys
                .filter { it.startsWith(CONV_KEY_PREFIX) }
                .mapNotNull { key ->
                    val peerId = key.removePrefix(CONV_KEY_PREFIX)
                    val conv = getConversation(peerId)
                    if (conv.isEmpty()) null else Pair(peerId, conv.last().timestamp)
                }
                .sortedByDescending { it.second }
                .map { it.first }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all conversation peer IDs", e)
            emptyList()
        }
    }

    /** Get last message for a peer (for preview in DM list). */
    fun getLastMessage(peerId: String): StoredMessage? = getConversation(peerId).lastOrNull()

    /** Persistently save a peer's custom display name. */
    fun savePeerAlias(peerId: String, alias: String) {
        if (alias.isBlank()) return
        if (alias.startsWith("Peer ") || alias == "Peer" || alias.startsWith("Android_") || alias.startsWith("Wi-Fi ")) return
        if (alias.contains(":") && alias.length >= 17) return
        try {
            prefs.edit().putString("alias_$peerId", alias.trim()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alias for $peerId", e)
        }
    }

    /** Retrieve a peer's saved custom display name if known. */
    fun getPeerAlias(peerId: String): String? {
        val saved = prefs.getString("alias_$peerId", null)
        return if (!saved.isNullOrBlank()) saved else null
    }

    /** Returns the best display name for a peer (custom display name if known, else short Node ID). */
    fun getPeerDisplayName(peerId: String): String {
        val saved = getPeerAlias(peerId)
        if (!saved.isNullOrBlank()) return saved
        // Clean up peerId: if it contains colons (e.g. Bluetooth MAC AA:BB:CC:DD:7A:AD), strip colons
        val cleanId = peerId.replace(":", "").uppercase()
        val shortId = if (cleanId.startsWith("NODE-")) {
            cleanId.removePrefix("NODE-").take(6)
        } else if (cleanId.length >= 4) {
            cleanId.takeLast(4)
        } else {
            cleanId
        }
        return "Peer $shortId"
    }
}

enum class MessageStatus {
    SENDING,
    PAUSED,
    SENT,
    DELIVERED
}

@Immutable
data class StoredMessage(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val text: String,
    val timestamp: Long,
    val hopCount: Int,
    val isMine: Boolean,
    val status: MessageStatus = if (isMine) MessageStatus.SENT else MessageStatus.DELIVERED
)
