package com.elcondor.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elcondor.app.ui.AppRoot
import com.elcondor.app.ui.theme.ElCondorTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as ElCondorApp

    /** Sin este permiso la cuenta atrás funciona igual; solo no se ve la notificación. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            ElCondorTheme {
                AppRoot(app)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // El usuario pudo iniciar/detener Shizuku o revocar el permiso desde su app.
        app.shell.refreshState()
    }
}
