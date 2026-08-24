# Citadel: Design & Architecture Review Report

This report evaluates the 15-document design suite for Citadel. The goal is to detect structural risks, inconsistencies, UX traps, and technical hurdles before writing code.

---

## 1. Identified Contradictions & Ambiguities

### A. Dynamic Night Accent Contrast vs. Accessibility
*   **The Issue**: `DesignLanguage.md` suggests shifting the primary interactive accent color from `Dawn Gold` (day) to `Twilight Violet` (night). 
*   **Risk**: `Twilight Violet` (`#6C63A8`) has a much lower contrast ratio on dark backgrounds than `Dawn Gold` (`#D4A84F`). Using it as an interactive text/button color at night will violate WCAG contrast standards, making the app difficult to read in bed.
*   **Resolution**: Keep `Dawn Gold` as the primary interactive accent (buttons, primary statuses) across all times of day. Reserve `Twilight Violet` for non-interactive decorative elements, companion glows, and magic moments.

### B. The Midnight Sweep Execution
*   **The Issue**: The documentation states tasks are archived "at midnight."
*   **Risk**: Scheduling a background worker (`WorkManager`) to wake up exactly at midnight to run database operations is unreliable due to Android's aggressive battery optimizations (Doze Mode). It also introduces unnecessary sync and CPU overhead.
*   **Resolution**: Implement a **Lazy Day-Sweep**. When the user launches the app, compare the current date with the saved "last_active_date". If they differ, execute the archiving sweep lazily on the main thread or a dispatcher IO before rendering today's UI.

---

## 2. UX & Psychological Risks

### A. The "Forgotten Promise" Trap
*   **The Issue**: Automatically sweeping unfinished tasks to the archive prevents shame, but if a task was critical (e.g., "Take medication"), the user might forget it exists.
*   **Risk**: Anxiety shifts from "having too many tasks" to "worrying that I forgot what was archived."
*   **Resolution**: Add a soft "Past Promises" tab in the archives, allowing users to tap on an archived task and choose "Re-enlist" to bring it back to today's active list with zero penalties.

### B. Ambient Motion Overload
*   **The Issue**: The progression layers describe campfire flames, smoke, falling leaves, twinkling stars, clouds, flying birds, and dynamic weather all happening simultaneously.
*   **Risk**: Too many simultaneous micro-animations create visual noise, violating the "calm and breathing" philosophy.
*   **Resolution**: Implement a **Dominant State System**. If it is raining, turn off the smoke/campfire sparkles and birds. If it is dawn, focus on fog clearing rather than wind and leaves. Keep active animations capped to a maximum of two concurrent layers.

---

## 3. Merging & Simplification Strategy

To prevent rule divergence, we should merge the 15 original documents into **9 consolidated files**:

| Original Files | Target Consolidations | Rationale |
| :--- | :--- | :--- |
| `Vision.md`, `NorthStar.md` | **`Vision.md`** | Fuses target identity with the North Star filter rule. |
| `DesignLanguage.md`, `VisualIdentity.md` | **`DesignLanguage.md`** | Fuses color theory with Jetpack Compose implementation details. |
| `Kingdom.md`, `KingdomProgression.md` | **`Kingdom.md`** | Merges environmental assets with progress states. |
| `MissionSystem.md`, `RewardSystem.md` | **`MissionSystem.md`** | Pairs task mechanics with the visual rewards. |
| `GameplayLoop.md` | Keep | Stands alone as the user flow timeline. |
| `Companion.md` | Keep | Stands alone as the dialogue policy. |
| `Notifications.md` | Keep | Stands alone as a copy-writing dictionary. |
| `Version1Scope.md`, `FutureIdeas.md` | **`Scope.md`** | Consolidates what we build now vs. what we build later. |
| `ArchitectureGuidelines.md` | Keep | Code structural integrity document. |

---

## 4. Design & Engineering Constraints (New Core Assets)

We have added two foundational documents that serve as strict, permanent constraints on all development stages:
1.  **[PlayerPersona.md](file:///d:/Citadel/docs/PlayerPersona.md)**: Defines the emotional timeline of the Commander (Day 1 to Year 1) and locks focus on anxiety reduction rather than traditional demographics.
2.  **[AntiGoals.md](file:///d:/Citadel/docs/AntiGoals.md)**: Formally documents the anti-retention mechanics (rejection of streaks, gamification indicators, dark patterns, and notification manipulation) to serve as a code gate. All future PRs must be checked against these constraints.
