package dev.atharva.citadel.ui.sanctuary

/**
 * The privacy policy, in the app itself.
 *
 * Kept word for word with PRIVACY.md and docs/privacy/index.html in the repository, which
 * is the copy the Play listing links to. If one changes, all three change.
 */
object PrivacyPolicy {

    const val EFFECTIVE = "27 September 2026"
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
            "The missions you write, the Chronicle of past days, your kingdom's totals and your " +
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
            "Whispers and the widget",
            "Whispers are written on your device from your own missions. On the lock screen they " +
                "show only a private version. If you add the home-screen widget, today's mission " +
                "titles appear on your home screen, where anyone who can see your screen can read them."
        ),
        Section(
            "Your control",
            "\"Let it go\" deletes a mission. Clearing Citadel's storage in system settings, or " +
                "uninstalling the app, removes everything it has stored. Whispers can be changed or " +
                "turned off at any time in the Sanctuary or in your system settings."
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
