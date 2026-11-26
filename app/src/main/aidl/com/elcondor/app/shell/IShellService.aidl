package com.elcondor.app.shell;

import android.os.Bundle;

/**
 * Interfaz del UserService que Shizuku ejecuta con UID de shell (2000) o root (0).
 *
 * Usamos Bundle como valor de retorno para no necesitar un Parcelable propio:
 * las claves están definidas en ShellResult.Keys.
 */
interface IShellService {

    /**
     * Código de transacción reservado por Shizuku: se invoca al hacer
     * unbindUserService(..., remove = true). La implementación debe salir del proceso.
     */
    void destroy() = 16777114;

    /** Ejecuta `sh -c command` y devuelve exitCode / stdout / stderr. */
    Bundle execute(String command, long timeoutMs) = 1;
}
