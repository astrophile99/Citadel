# Citadel Design Language

## Visual Foundations

Citadel's visual language is built on serenity, breathing room, and atmospheric change. It balances modern Material 3 architecture with the aesthetic restraint of Nothing OS and the natural warmth of *Monument Valley* and *Journey*.

---

## The Palette: Time-Aware Obsidian
The theme changes depending on the Commander's local time, allowing the Citadel to live in the same day/night cycle as the user.

| Theme | Time Range | Background | Surface | Accent | Mood |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **🌅 Dawn** | 6:00 AM - 11:59 AM | `#0D1420` (Dawn Blue) | `#141C2B` | Dawn Gold | Awakening, hopeful |
| **☀️ Day** | 12:00 PM - 5:59 PM | `#0D1117` (Obsidian) | `#18212D` | Dawn Gold | Grounded, focused |
| **🌆 Dusk** | 6:00 PM - 8:59 PM | `#151111` (Warm Stone) | `#221C1C` | Dawn Gold | Warmth, safety |
| **🌙 Night** | 9:00 PM - 5:59 AM | `#07050A` (Midnight) | `#110E18` | Twilight Violet | Quiet, starlit rest |

---

## Typography Hierarchy
We merge classic serif fonts with modern sans-serifs to create a "Tactical Chronicle" aesthetic:

*   **Display & Headlines (`FontFamily.Serif`)**: Used for titles, narrative quotes, and major section headings. Emphasizes the fantasy chronicle/journal feeling.
*   **Body & Buttons (`FontFamily.SansSerif`)**: Standard system sans-serif. Highly readable for task management, input, and details.
*   **Labels & Metadata (`FontFamily.SansSerif` Uppercase, tracked out)**: Small, clean annotations representing notes, dates, and subtle markers.

---

## Elements & Elevation
*   **Muted Radii**: Surfaces use soft, rounded corners (`16dp` to `24dp`) to feel organic rather than sharp and mechanical.
*   **Minimal Elevation**: We do not use harsh shadows. Elevation is represented by subtle changes in surface color (e.g., nesting a slightly lighter surface within the obsidian background) and thin borders (`#6B7280` with low opacity).
*   **Atmospheric Gradients**: Gradients are dark, slow, and linear. They should only be used to transition between backgrounds or overlay time-of-day tints on the silhouette artwork.
