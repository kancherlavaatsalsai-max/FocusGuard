package com.focusguard.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.ui.navigation.FocusGuardNavGraph
import com.focusguard.app.ui.theme.FocusGuardTheme
import com.focusguard.app.util.PermissionUtils
import com.focusguard.app.widget.ClatWidgets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        PermissionUtils.startMonitor(this)
        ClatWidgets.updateAll(this)

        setContent {
            val dark by prefs.darkTheme.collectAsState(initial = true)
            FocusGuardTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FocusGuardNavGraph()
                }
            }
        }
    }
}
