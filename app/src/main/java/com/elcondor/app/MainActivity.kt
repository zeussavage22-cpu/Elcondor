package com.elcondor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elcondor.app.ui.AppRoot
import com.elcondor.app.ui.theme.ElCondorTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as ElCondorApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
