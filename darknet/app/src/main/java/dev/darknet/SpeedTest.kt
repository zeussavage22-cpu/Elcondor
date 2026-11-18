package dev.darknet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/** Measures this phone's own connection: ping, then download and upload over parallel streams. */
object SpeedTest {
    enum class Phase { IDLE, PING, DOWNLOAD, UPLOAD, DONE, FAILED }

    data class Result(
        val phase: Phase = Phase.IDLE,
        val liveMbps: Double = 0.0,
        val pingMs: Long? = null,
        val downMbps: Double? = null,
        val upMbps: Double? = null,
        val error: String = "",
    ) {
        val running get() = phase == Phase.PING || phase == Phase.DOWNLOAD || phase == Phase.UPLOAD
    }

    private const val BASE = "https://speed.cloudflare.com"
    private const val STREAMS = 4
    private const val PHASE_MS = 8_000L
    private const val UPLOAD_SIZE = 8_000_000

    private val mutable = MutableStateFlow(Result())
    val state: StateFlow<Result> = mutable.asStateFlow()

    fun mbps(bytes: Long, ms: Long) = if (ms <= 0) 0.0 else bytes * 8.0 / 1e6 / (ms / 1000.0)

    suspend fun run() {
        if (mutable.value.running) return
        mutable.value = Result(Phase.PING)
        try {
            val ping = withContext(Dispatchers.IO) { ping() }
            mutable.value = mutable.value.copy(pingMs = ping, phase = Phase.DOWNLOAD)
            val down = throughput(upload = false)
            mutable.value = mutable.value.copy(downMbps = down, phase = Phase.UPLOAD, liveMbps = 0.0)
            val up = throughput(upload = true)
            mutable.value = mutable.value.copy(upMbps = up, phase = Phase.DONE, liveMbps = 0.0)
        } catch (e: CancellationException) {
            mutable.value = Result()
            throw e
        } catch (e: Throwable) {
            mutable.value = mutable.value.copy(phase = Phase.FAILED, liveMbps = 0.0, error = e.message ?: "error")
        }
    }

    private fun ping(): Long {
        val samples = (0 until 5).mapNotNull {
            runCatching {
                val t = System.nanoTime()
                open("$BASE/__down?bytes=0").run { inputStream.use { s -> s.readBytes() }; disconnect() }
                (System.nanoTime() - t) / 1_000_000
            }.getOrNull()
        }
        check(samples.isNotEmpty()) { "Sin conexión" }
        return (if (samples.size > 1) samples.drop(1) else samples).min()
    }

    private suspend fun throughput(upload: Boolean): Double = coroutineScope {
        val moved = AtomicLong()
        val start = System.currentTimeMillis()
        val end = start + PHASE_MS
        val meter = launch {
            var lastBytes = 0L
            var lastAt = start
            while (isActive) {
                delay(250)
                val now = System.currentTimeMillis()
                val b = moved.get()
                mutable.value = mutable.value.copy(liveMbps = mbps(b - lastBytes, now - lastAt))
                lastBytes = b
                lastAt = now
            }
        }
        (0 until STREAMS).map {
            async(Dispatchers.IO) {
                while (System.currentTimeMillis() < end) {
                    runCatching { if (upload) uploadOnce(moved, end) else downloadOnce(moved, end) }
                        .onFailure { if (moved.get() == 0L) throw it }
                }
            }
        }.awaitAll()
        meter.cancel()
        check(moved.get() > 0) { "No se transfirieron datos" }
        mbps(moved.get(), System.currentTimeMillis() - start)
    }

    private fun downloadOnce(moved: AtomicLong, end: Long) {
        val c = open("$BASE/__down?bytes=50000000")
        try {
            c.inputStream.use { input ->
                val buf = ByteArray(64 * 1024)
                while (System.currentTimeMillis() < end) {
                    val n = input.read(buf)
                    if (n < 0) break
                    moved.addAndGet(n.toLong())
                }
            }
        } finally {
            c.disconnect()
        }
    }

    private val payload by lazy { Random.nextBytes(64 * 1024) }

    private fun uploadOnce(moved: AtomicLong, end: Long) {
        val c = open("$BASE/__up").apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(UPLOAD_SIZE)
        }
        var sent = 0
        try {
            c.outputStream.use { out ->
                while (sent < UPLOAD_SIZE && System.currentTimeMillis() < end) {
                    val n = minOf(payload.size, UPLOAD_SIZE - sent)
                    out.write(payload, 0, n)
                    sent += n
                    moved.addAndGet(n.toLong())
                }
            }
            if (sent >= UPLOAD_SIZE) runCatching { c.inputStream.use { it.readBytes() } }
        } finally {
            c.disconnect()
        }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        useCaches = false
    }
}
