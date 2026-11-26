# El UserService de Shizuku se instancia por reflexión (por nombre de clase)
# dentro del proceso remoto: no puede renombrarse ni eliminarse.
-keep class com.elcondor.app.shell.ShellService { *; }
-keep class com.elcondor.app.shell.IShellService { *; }
-keep class com.elcondor.app.shell.IShellService$* { *; }
