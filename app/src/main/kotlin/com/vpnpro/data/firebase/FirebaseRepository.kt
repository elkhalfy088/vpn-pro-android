package com.vpnpro.data.firebase

import com.google.firebase.database.*
import com.vpnpro.data.model.Server
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance()
    private val serversRef = db.getReference("servers")

    /** Real-time stream of all servers */
    fun serversFlow(): Flow<List<Server>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val list = mutableListOf<Server>()
                snap.children.forEach { child ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = child.value as? Map<String, Any?> ?: return@forEach
                        val server = Server.fromMap(child.key ?: "", map)
                        if (server.enabled) list.add(server)
                    } catch (_: Exception) {}
                }
                trySend(list.sortedByDescending { it.addedAt })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        serversRef.addValueEventListener(listener)
        awaitClose { serversRef.removeEventListener(listener) }
    }

    /** Add a new server — visible to all users immediately */
    suspend fun addServer(server: Server) {
        val key = serversRef.push().key ?: return
        val newServer = server.copy(id = key)
        serversRef.child(key).setValue(newServer.toMap())
    }

    /** Toggle server enabled state (admin only by Firebase rules) */
    suspend fun setServerEnabled(id: String, enabled: Boolean) {
        serversRef.child(id).child("enabled").setValue(enabled)
    }
}
