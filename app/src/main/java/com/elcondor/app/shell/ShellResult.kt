package com.elcondor.app.shell

import android.os.Bundle

/** Resultado crudo de un comando ejecutado en el proceso privilegiado. */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val truncated: Boolean = false,
) {
    val isSuccess: Boolean get() = exitCode == 0 && !timedOut

    fun toBundle(): Bundle = Bundle().apply {
        putInt(Keys.EXIT_CODE, exitCode)
        putString(Keys.STDOUT, stdout)
        putString(Keys.STDERR, stderr)
        putBoolean(Keys.TIMED_OUT, timedOut)
        putBoolean(Keys.TRUNCATED, truncated)
    }

    /** Claves compartidas entre el proceso de la app y el UserService. */
    object Keys {
        const val EXIT_CODE = "exit_code"
        const val STDOUT = "stdout"
        const val STDERR = "stderr"
        const val TIMED_OUT = "timed_out"
        const val TRUNCATED = "truncated"
    }

    companion object {
        /** Código usado cuando el proceso no terminó por sí mismo (timeout). */
        const val EXIT_TIMEOUT = -1

        fun fromBundle(bundle: Bundle): ShellResult = ShellResult(
            exitCode = bundle.getInt(Keys.EXIT_CODE, EXIT_TIMEOUT),
            stdout = bundle.getString(Keys.STDOUT).orEmpty(),
            stderr = bundle.getString(Keys.STDERR).orEmpty(),
            timedOut = bundle.getBoolean(Keys.TIMED_OUT),
            truncated = bundle.getBoolean(Keys.TRUNCATED),
        )
    }
}
