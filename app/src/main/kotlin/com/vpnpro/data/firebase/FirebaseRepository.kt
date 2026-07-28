package com.vpnpro.data.firebase

import com.google.firebase.database.*
import com.vpnpro.data.model.Server
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance()
    private val serversRef = db.getReference("servers")

    fun serversFlow(): Flow<List<Server>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Server>()
                for (child in snapshot.children) {
                    val s = child.getValue(Server::class.java)
                    if (s != null) list.add(s.copy(id = child.key ?: s.id))
                }
                trySend(list.sortedByDescending { it.addedAt })
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        serversRef.addValueEventListener(listener)
        awaitClose { serversRef.removeEventListener(listener) }
    }

    suspend fun addServer(server: Server) {
        val key = if (server.id.isBlank()) serversRef.push().key ?: return else server.id
        serversRef.child(key).setValue(server.copy(id = key)).await()
    }

    suspend fun updateServer(server: Server) {
        require(server.id.isNotBlank()) { "Server id required for update" }
        serversRef.child(server.id).setValue(server).await()
    }

    suspend fun deleteServer(serverId: String) {
        require(serverId.isNotBlank()) { "Server id required for delete" }
        serversRef.child(serverId).removeValue().await()
    }

    suspend fun incrementUsage(serverId: String) {
        if (serverId.isBlank()) return
        val ref = serversRef.child(serverId).child("usageCount")
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val current = data.getValue(Int::class.java) ?: 0
                data.value = current + 1
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }
}
