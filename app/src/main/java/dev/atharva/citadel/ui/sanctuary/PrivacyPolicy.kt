package dev.atharva.citadel.ui.sanctuary

/**
 * The privacy policy, in the app itself.
 *
 * Kept word for word with PRIVACY.md and docs/privacy/index.html in the repository, which
 * is the copy the Play listing links to. If one changes, all three change.
 */
object PrivacyPolicy {

    const val EFFECTIVE = "20 October 2026"
    const val CONTACT_URL = "https://github.com/astrophile99/Citadel/issues"

    class Section(val heading: String, val body: String)

    val sections = listOf(
        Section(
            "The short version",
            "Citadel keeps everything on your phone. There is no account, no advertising, " +
                "no analytics and no tracking — and the app does not have permission to use the internet."
        ),
        Section(
            "What Citadel stores",
            "The tasks you write, your history of past days, your castle's totals and your " +
                "settings. All of it lives in Citadel's private storage on your device, where other " +
                "apps cannot read it."
        ),
        Section(
            "What leaves your device",
            "Nothing is sent to the developer or to anyone else. If you have Android's own device " +
                "backup turned on, Android may include Citadel's data in that backup; it is handled " +
                "by Google under your device's backup settings, not by Citadel."
        ),
        Section(
            "Reminders and the widget",
            "Reminders are written on your device from your own tasks. On the lock screen they " +
                "show only a private version. If you add the home-screen widget, today's task " +
                "titles appear on your home screen, where anyone who can see your screen can read them."
        ),
        Section(
            "Your control",
            "\"Delete\" removes a task for good. Clearing Citadel's storage in system settings, or " +
                "uninstalling the app, removes everything it has stored. Reminders can be changed or " +
                "turned off at any time in Settings or in your phone's settings."
        ),
        Section(
            "Children",
            "Citadel is not directed at children under 13, and it collects no personal " +
                "information from anyone."
        ),
        Section(
            "Changes",
            "If this policy changes, the new version will be published at the same address with a " +
                "new effective date."
        ),
        Section(
            "Contact",
            "Questions about privacy can be raised at $CONTACT_URL."
        )
    )
}
