# Citadel Visual Identity

## The Art Style: Atmospheric Silhouette
Citadel's visual centerpiece is a vector-based, dynamic landscape illustration. It relies on layered, high-contrast flat shapes and gradients to create depth, light, and weather.

```
+------------------------------------------+
|  [Sky/Stars/Clouds Gradient Layer]       |
|                                          |
|         /\ (Mountain Silhouettes)        |
|        /  \                              |
|       /____\      [Tower Silhouette]     |
|                   +---+                  |
|                   | o | <- [Campfire]    |
|                   |   |                  |
|  [Hills Layer]    +---+                  |
|  [Hearth Bench & Companion]              |
+------------------------------------------+
```

---

## The Layered Canvas System

We render the Citadel using a custom Jetpack Compose `Canvas` or overlapping `Image` layers using vector graphics. This lets us edit visual details on-the-fly based on StateFlow variables:

1.  **Layer 0 (Sky & Stars)**: Changes color gradient based on local time. Star particles fade in during Night theme.
2.  **Layer 1 (Distant Mountains)**: Slow-moving vector silhouettes that shift alpha when weather becomes misty.
3.  **Layer 2 (The Keep / Citadel Silhouettes)**: Static architectural nodes. Draws decorative features (flags, wall lanterns) depending on completed daily missions.
4.  **Layer 3 (Foreground camp & fire)**: Renders the active campfire particle effects and the static companion position.
5.  **Layer 4 (Weather overlays)**: Simple rain/snow particle streams drawn over the entire canvas when weather states are active.

---

## M3 Theme Mapping

We map our time-aware color palette to the standard Material 3 color system. This ensures that any standard Material component we use (cards, buttons, text fields) automatically adapts to the time of day:

*   `MaterialTheme.colorScheme.background` -> Time-specific Obsidian color.
*   `MaterialTheme.colorScheme.surface` -> Time-specific Surface color.
*   `MaterialTheme.colorScheme.primary` -> Dawn Gold. Used for primary branding and accentuation.
*   `MaterialTheme.colorScheme.secondary` -> Twilight Violet. Used for narrative cues, companion cards, and ambient states.
*   `MaterialTheme.colorScheme.tertiary` -> Ancient Forest. Used to represent successful restorations (completed missions).
