package com.cpen321.usermanagement.network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import java.util.concurrent.atomic.AtomicBoolean


data class SocketEvent(val name: String, val payload: JSONObject?)

class SocketClient(
    private val baseUrl: String
) {
    // SharedScope for socket callbacks -> emits onto flow
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null

    // simple hot stream for events
    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val connected = AtomicBoolean(false)

    /**
     * Connect using a JWT token. The token is passed in the socket query string
     * (backend must accept either query or auth header for handshake).
     */
    fun connect(token: String) {
        // If already connected with same socket, do nothing (caller is responsible for reusing)
        if (connected.get()) return
        // Mask token for logs and ensure Bearer prefix
        fun maskToken(t: String): String {
            val raw = t.removePrefix("Bearer ").trim()
            if (raw.length <= 8) return "***"
            return raw.take(6) + "..." + raw.takeLast(4)
        }

        // Normalize token: accept 'Bearer <token>' or 'Bearer: <token>' and strip prefixes
        val rawToken = token.removePrefix("Bearer ").removePrefix("Bearer:").trim()
        val authHeader = "Bearer $rawToken"
        Log.d("SocketClient", "Connecting to socket at $baseUrl with token=${maskToken(authHeader)}")
        try {
            val opts = IO.Options()
            // Send token in the Authorization header to make reconnections carry auth
            opts.extraHeaders = mapOf("Authorization" to listOf(authHeader))
            // Also populate handshake auth for Socket.IO servers that read handshake.auth.token
            opts.auth = mapOf("token" to rawToken)
            socket = IO.socket(baseUrl, opts)
            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    connected.set(true)
                    scope.launch { _events.emit(SocketEvent("connect", null)) }
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    connected.set(false)
                    scope.launch { _events.emit(SocketEvent("disconnect", null)) }
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val err = args.firstOrNull()
                    scope.launch { _events.emit(SocketEvent("connect_error", JSONObject().put("error", err?.toString()))) }
                }

                // Listen to the order.created event
                on("order.created") { args ->
                    val payload = args?.firstOrNull() as? org.json.JSONObject
                    scope.launch { _events.emit(SocketEvent("order.created", payload)) }
                }

                // Also forward job.updated and order.updated if desired
                on("job.updated") { args ->
                    val payload = args?.firstOrNull() as? org.json.JSONObject
                    scope.launch { _events.emit(SocketEvent("job.updated", payload)) }
                }

                on("order.updated") { args ->
                    val payload = args?.firstOrNull() as? org.json.JSONObject
                    scope.launch { _events.emit(SocketEvent("order.updated", payload)) }
                }

                connect()
            }
        } catch (e: URISyntaxException) {
            // emit connect error
            scope.launch { _events.emit(SocketEvent("connect_error", JSONObject().put("error", e.message))) }
        }
    }

    /**
     * Disconnect and clear socket.
     */
    fun disconnect() {
        socket?.let {
            try {
                it.disconnect()
                it.off()
            } catch (_: Exception) { /* ignore */ }
        }
        socket = null
        connected.set(false)
    }

    /**
     * Reconnect with a new token (useful when token refresh happens).
     */
    fun reconnectWithToken(newToken: String) {
        disconnect()
        connect(newToken)
    }
}