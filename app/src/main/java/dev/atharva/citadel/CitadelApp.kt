package dev.atharva.citadel

import android.app.Application
import dev.atharva.citadel.core.time.CitadelClock
import dev.atharva.citadel.core.time.ClockProvider
import dev.atharva.citadel.data.CitadelRepository
import dev.atharva.citadel.data.SettingsStore
import dev.atharva.citadel.data.store.CitadelStore
import dev.atharva.citadel.system.Whispers
import dev.atharva.citadel.widget.CitadelWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * One object graph, assembled by hand.
 *
 * A dependency-injection framework would be four moving parts and a code-generation step
 * for what is, honestly, four singletons. If the graph ever grows past what fits on this
 * screen, that is the moment to reach for Hilt — not before.
 */
class CitadelApp : Application() {

    lateinit var container: CitadelContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = CitadelContainer(this)
        Whispers.ensureChannels(this)

        // The home-screen widget follows every change the Commander makes. Changes that
        // arrive in a burst (typing, several ticks in a row) are drawn once, at the end.
        appScope.launch {
            container.repository.data.drop(1).collectLatest { data ->
                delay(350)
                CitadelWidget.refreshAll(this@CitadelApp, data)
            }
        }
    }
}

class CitadelContainer(app: Application) {
    val clock: CitadelClock = ClockProvider.create(app)
    val settings: SettingsStore = SettingsStore(app)
    val store: CitadelStore = CitadelStore(app)
    val repository: CitadelRepository = CitadelRepository(store, clock)
}
