package dev.atharva.citadel.data.store

import android.content.Context
import android.util.Log
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Durable memory for the Citadel.
 *
 * This is deliberately a plain JSON document rather than a database. The whole app is a
 * few hundred short records that are always read as one snapshot, so a table engine
 * would add build complexity and migration ceremony for no benefit. Everything behind
 * [dev.atharva.citadel.data.CitadelRepository] is an implementation detail — if this
 * ever outgrows a file, Room can slot in here without the UI noticing.
 *
 * Writes are atomic: a temp file is written and then renamed, so a process death
 * mid-save can never leave a half-written Citadel behind.
 */
interface CitadelStorage {
    suspend fun read(): CitadelData
    suspend fun write(data: CitadelData)
}

class CitadelStore(private val context: Context) : CitadelStorage {

    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val tempFile: File get() = File(context.filesDir, "$FILE_NAME.tmp")

    override suspend fun read(): CitadelData = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching CitadelData.Empty
            val raw = file.readText()
            if (raw.isBlank()) return@runCatching CitadelData.Empty
            decode(JSONObject(raw))
        }.getOrElse { error ->
            // A corrupt file must never cost the Commander their kingdom silently.
            Log.e(TAG, "Could not read the Citadel; keeping a copy and starting fresh", error)
            runCatching { file.copyTo(File(context.filesDir, "$FILE_NAME.recovered"), overwrite = true) }
            CitadelData.Empty
        }
    }

    override suspend fun write(data: CitadelData) = withContext(Dispatchers.IO) {
        runCatching {
            tempFile.writeText(encode(data).toString())
            if (file.exists()) file.delete()
            tempFile.renameTo(file)
        }.onFailure { Log.e(TAG, "Could not save the Citadel", it) }
        Unit
    }

    // ---- encoding -------------------------------------------------------------------

    private fun encode(data: CitadelData): JSONObject = JSONObject().apply {
        put(KEY_VERSION, SCHEMA_VERSION)
        put(KEY_MISSIONS, JSONArray().apply { data.missions.forEach { put(encodeMission(it)) } })
        put(KEY_CHRONICLE, JSONArray().apply { data.chronicle.forEach { put(encodeEntry(it)) } })
        put(KEY_KINGDOM, encodeKingdom(data.kingdom))
    }

    private fun encodeMission(m: Mission) = JSONObject().apply {
        put("id", m.id)
        put("title", m.title)
        put("impact", m.impact.name)
        put("status", m.status.name)
        put("dayKey", m.dayKey)
        put("recurrence", m.recurrence.name)
        put("createdAt", m.createdAt)
        m.completedAt?.let { put("completedAt", it) }
        put("carried", m.carried)
        put("counted", m.counted)
    }

    private fun encodeEntry(e: ChronicleEntry) = JSONObject().apply {
        put("dayKey", e.dayKey)
        put("kept", JSONArray(e.kept))
        put("waiting", JSONArray(e.waiting))
        put("prepared", e.prepared)
        put("note", e.note)
    }

    private fun encodeKingdom(k: KingdomState) = JSONObject().apply {
        put("foundedDayKey", k.foundedDayKey)
        put("totalKept", k.totalKept)
        put("daysProtected", k.daysProtected)
        put("lastActiveDayKey", k.lastActiveDayKey)
        put("lastProtectedDayKey", k.lastProtectedDayKey)
        put("lastKeptAtMillis", k.lastKeptAtMillis)
        put("hasArrived", k.hasArrived)
    }

    // ---- decoding -------------------------------------------------------------------

    private fun decode(json: JSONObject): CitadelData = CitadelData(
        missions = json.optJSONArray(KEY_MISSIONS).map { decodeMission(it) }.filterNotNull(),
        chronicle = json.optJSONArray(KEY_CHRONICLE).map { decodeEntry(it) }.filterNotNull(),
        kingdom = json.optJSONObject(KEY_KINGDOM)?.let { decodeKingdom(it) } ?: KingdomState()
    )

    private fun decodeMission(o: JSONObject): Mission? {
        val title = o.optString("title").takeIf { it.isNotBlank() } ?: return null
        return Mission(
            id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            title = title,
            impact = o.optString("impact").toEnum(MissionImpact.entries, MissionImpact.MODERATE),
            status = o.optString("status").toEnum(MissionStatus.entries, MissionStatus.ACTIVE),
            dayKey = o.optString("dayKey"),
            recurrence = o.optString("recurrence").toEnum(Recurrence.entries, Recurrence.ONCE),
            createdAt = o.optLong("createdAt"),
            completedAt = if (o.has("completedAt")) o.optLong("completedAt") else null,
            carried = o.optInt("carried"),
            counted = o.optBoolean("counted")
        )
    }

    private fun decodeEntry(o: JSONObject) = ChronicleEntry(
        dayKey = o.optString("dayKey"),
        kept = o.optJSONArray("kept").strings(),
        waiting = o.optJSONArray("waiting").strings(),
        prepared = o.optInt("prepared"),
        note = o.optString("note")
    )

    private fun decodeKingdom(o: JSONObject) = KingdomState(
        foundedDayKey = o.optString("foundedDayKey"),
        totalKept = o.optInt("totalKept"),
        daysProtected = o.optInt("daysProtected"),
        lastActiveDayKey = o.optString("lastActiveDayKey"),
        lastProtectedDayKey = o.optString("lastProtectedDayKey"),
        lastKeptAtMillis = o.optLong("lastKeptAtMillis"),
        hasArrived = o.optBoolean("hasArrived")
    )

    private fun <T> JSONArray?.map(transform: (JSONObject) -> T?): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(transform) }
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i -> optString(i).takeIf { it.isNotBlank() } }
    }

    private fun <T : Enum<T>> String?.toEnum(values: List<T>, fallback: T): T =
        values.firstOrNull { it.name == this } ?: fallback

    private companion object {
        const val TAG = "CitadelStore"
        const val FILE_NAME = "citadel.json"
        const val SCHEMA_VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_MISSIONS = "missions"
        const val KEY_CHRONICLE = "chronicle"
        const val KEY_KINGDOM = "kingdom"
    }
}
