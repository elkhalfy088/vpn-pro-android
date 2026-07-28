package akh.vpn.dd.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseManager {

    private val db = FirebaseDatabase.getInstance()
    private val configRef = db.getReference("vpn_config")

    /** Listen to config changes in real-time */
    fun configFlow(): Flow<VpnConfig?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val config = VpnConfig(
                    serverHost  = snapshot.child("server_host").getValue(String::class.java) ?: "",
                    serverPort  = snapshot.child("server_port").getValue(Int::class.java) ?: 8443,
                    decoyDomain = snapshot.child("decoy_domain").getValue(String::class.java)
                        ?: "www.facebook.com",
                    setupDone   = snapshot.child("setup_done").getValue(Boolean::class.java) ?: false
                )
                trySend(config)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        configRef.addValueEventListener(listener)
        awaitClose { configRef.removeEventListener(listener) }
    }

    /** One-shot read */
    suspend fun readConfig(): VpnConfig? =
        suspendCancellableCoroutine { cont ->
            configRef.get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) { cont.resume(null); return@addOnSuccessListener }
                    val config = VpnConfig(
                        serverHost  = snapshot.child("server_host").getValue(String::class.java) ?: "",
                        serverPort  = snapshot.child("server_port").getValue(Int::class.java) ?: 8443,
                        decoyDomain = snapshot.child("decoy_domain").getValue(String::class.java)
                            ?: "www.facebook.com",
                        setupDone   = snapshot.child("setup_done").getValue(Boolean::class.java) ?: false
                    )
                    cont.resume(config)
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /** Save config to Firebase (first-run setup) */
    suspend fun saveConfig(config: VpnConfig) {
        suspendCancellableCoroutine<Unit> { cont ->
            val map = mapOf(
                "server_host"  to config.serverHost,
                "server_port"  to config.serverPort,
                "decoy_domain" to config.decoyDomain,
                "setup_done"   to true
            )
            configRef.setValue(map)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
