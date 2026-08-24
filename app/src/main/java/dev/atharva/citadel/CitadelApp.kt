package dev.atharva.citadel

import android.app.Application
import dev.atharva.citadel.core.time.CitadelClock
import dev.atharva.citadel.core.time.SystemClock
import dev.atharva.citadel.data.CitadelRepository
import dev.atharva.citadel.data.SettingsStore
import dev.atharva.citadel.data.store.CitadelStore
import dev.atharva.citadel.system.Whispers

/**
 * One object graph, assembled by hand.
 *
 * A dependency-injection framework would be four moving parts and a code-generation step
 * for what is, honestly, three singletons. If the graph ever grows past what fits on this
 * screen, that is the moment to reach for Hilt — not before.
 */
class CitadelApp : Application() {

    lateinit var container: CitadelContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = CitadelContainer(this)
        Whispers.ensureChannel(this)
    }
}

class CitadelContainer(app: Application) {
    val clock: CitadelClock = SystemClock
    val settings: SettingsStore = SettingsStore(app)
    val repository: CitadelRepository = CitadelRepository(CitadelStore(app), clock)
}
