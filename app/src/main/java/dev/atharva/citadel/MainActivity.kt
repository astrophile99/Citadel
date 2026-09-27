package dev.atharva.citadel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.atharva.citadel.domain.WhisperRoute
import dev.atharva.citadel.system.Presence
import dev.atharva.citadel.system.Whispers
import dev.atharva.citadel.ui.CitadelShell
import dev.atharva.citadel.ui.CitadelViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CitadelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge with genuinely transparent bars, and dark icons forced off.
        // The Citadel is always dark, so the system must never decide to draw a light
        // scrim over the sky based on the device's day/night setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        // A recreated activity has already acted on the intent that launched it.
        if (savedInstanceState == null) followWhisper(intent)
        setContent { CitadelShell(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        followWhisper(intent)
    }

    override fun onStart() {
        super.onStart()
        Presence.inForeground = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }

    override fun onStop() {
        Presence.inForeground = false
        viewModel.onBackground()
        super.onStop()
    }

    /** A tapped whisper may ask for a particular place — the ritual, or a new promise. */
    private fun followWhisper(intent: Intent?) {
        val name = intent?.getStringExtra(Whispers.EXTRA_ROUTE) ?: return
        WhisperRoute.entries.firstOrNull { it.name == name }?.let(viewModel::open)
        intent.removeExtra(Whispers.EXTRA_ROUTE)
    }
}
