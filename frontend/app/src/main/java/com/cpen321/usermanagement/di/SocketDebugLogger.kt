package com.cpen321.usermanagement.di

import android.util.Log
import com.cpen321.usermanagement.network.SocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Small debug helper that subscribes to SocketClient.events and logs them to Logcat.
 * Instantiating this class starts the subscription; keep it simple and lightweight.
 */
class SocketDebugLogger(
    private val socketClient: SocketClient
) {
    companion object { private const val TAG = "SocketDebugLogger" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        socketClient.events
            .onEach { ev ->
                try {
                    val payload = ev.payload
                    // If connect_error, try to unwrap nested structures for clarity
                    if (ev.name == "connect_error") {
                        val raw = payload?.optString("error")
                        Log.d(TAG, "Socket event: ${ev.name} payload=$raw")
                    } else {
                        Log.d(TAG, "Socket event: ${ev.name} payload=${payload?.toString()}")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to log socket event ${ev.name}", t)
                }
            }
            .launchIn(scope)
    }
}
