package com.elcondor.app.shell

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * UserService de Shizuku.
 *
 * ¡OJO! Esta clase NO corre en el proceso de la app: Shizuku la instancia en un
 * proceso aparte lanzado vía `app_process` con UID shell (2000) o root (0).
 * Por eso:
 *  - no tiene acceso a la Application, ViewModels, ni singletons de la app;
 *  - todo lo que hace se ejecuta con los privilegios de shell;
 *  - debe mantenerse con `@Keep` / reglas de ProGuard (se carga por nombre).
 *
 * Es la alternativa recomendada a `Shizuku.newProcess()`, que es privado
 * desde la API 13 y está marcado para eliminación.
 */
@Keep
class ShellService() : IShellService.Stub() {

    /** Shizuku ≥ 13 prefiere este constructor si existe; el Context es del sistema. */
    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun destroy() {
        Log.i(TAG, "destroy() solicitado por Shizuku, saliendo del proceso")
        exitProcess(0)
    }

    override fun execute(command: String, timeoutMs: Long): Bundle {
        val process = try {
            ProcessBuilder("sh", "-c", command).start()
        } catch (t: Throwable) {
            // IllegalStateException cruza el binder y llega como tal al cliente.
            throw IllegalStateException("No se pudo iniciar el proceso: ${t.message}")
        }

        // No enviamos nada por stdin: lo cerramos para que comandos que esperan
        // entrada (p. ej. `pm install` sin argumentos) no se queden colgados.
        runCatching { process.outputStream.close() }

        // stdout y stderr se leen en paralelo: si leemos uno solo y el otro llena
        // su buffer (~64 KB), el proceso hijo se bloquea → deadlock.
        val stdout = StreamCollector(process.inputStream)
        val stderr = StreamCollector(process.errorStream)

        val finished = process.waitFor(timeoutMs.coerceAtLeast(1), TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()

        stdout.join(READER_JOIN_MS)
        stderr.join(READER_JOIN_MS)

        return ShellResult(
            exitCode = if (finished) process.exitValue() else ShellResult.EXIT_TIMEOUT,
            stdout = stdout.text,
            stderr = stderr.text,
            timedOut = !finished,
            truncated = stdout.truncated || stderr.truncated,
        ).toBundle()
    }

    /**
     * Lee un stream en un hilo propio, con tope de tamaño para no exceder
     * el límite de transacción de Binder (~1 MB compartido por proceso).
     */
    private class StreamCollector(stream: InputStream) {
        private val buffer = StringBuilder()
        @Volatile var truncated = false
            private set

        private val worker = thread(isDaemon = true, name = "shell-reader") {
            try {
                stream.bufferedReader().use { reader ->
                    val chunk = CharArray(8 * 1024)
                    while (true) {
                        val read = reader.read(chunk)
                        if (read < 0) break
                        synchronized(buffer) {
                            val room = MAX_OUTPUT_CHARS - buffer.length
                            if (room > 0) buffer.appendRange(chunk, 0, minOf(read, room))
                            if (read > room) truncated = true
                        }
                    }
                }
            } catch (_: Throwable) {
                // El stream se cierra al destruir el proceso por timeout: es esperable.
            }
        }

        val text: String get() = synchronized(buffer) { buffer.toString() }

        fun join(millis: Long) = worker.join(millis)
    }

    private companion object {
        const val TAG = "ElCondorShellService"
        const val MAX_OUTPUT_CHARS = 256 * 1024
        const val READER_JOIN_MS = 2_000L
    }
}
