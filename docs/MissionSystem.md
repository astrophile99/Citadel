# Citadel Mission System: Promises of the Commander

## Core Concept
Tasks in Citadel are named **Missions**. They represent promises the Commander makes to keep the kingdom safe. 

Unlike standard checklist managers, missions are treated as narrative agreements with the self.

---

## Mission Attributes

1.  **Title**: A clean, descriptive action statement. 
    *   *Example*: "Study 30 minutes of Kotlin," "Go for a brief walk," "Drink water at noon."
2.  **Difficulty (Semantic Impact)**: In Citadel, difficulty does not mean complexity or suffering. It represents **emotional importance** and **restorative impact**:
    *   **Minor (Skirmish)**: Light, quick tasks. (e.g., tidying a desk). Keeps the camp running.
    *   **Moderate (Fortification)**: Medium-effort tasks. Strengthens the Citadel's foundations.
    *   **Major (Expedition)**: Crucial, foundational tasks. (e.g., completing an interview, going to the gym). Restores significant hope and lights up major parts of the kingdom.
3.  **Recurrence**: Daily promises vs. one-time tasks.
4.  **Status**: Active, Complete, or Archived.

---

## The Workflow of a Mission

### 1. Enlistment (Creation)
When creating a mission, the UI remains minimalist. The user enters a title and selects the level of impact (Minor, Moderate, Major). The design avoids micro-managing due dates, subtasks, and reminders unless requested.

### 2. Execution & Focus
Missions are listed on the main dashboard under the Citadel illustration. They are clean cards with soft borders, separated by generous spacing.

### 3. Restoration (Completion)
When a mission is checked:
*   No loud checklists. No checkmarks that turn bright neon.
*   The card gently dissolves or fades into an ivory-green tint.
*   A subtle ripple animation travels up to the Citadel illustration, updating its visual state (e.g., stoking the campfire, lighting a lantern, clearing a layer of mist).

### 4. Transition (The Midnight Sweep)
At midnight (or when the app is first opened the next day):
*   Uncompleted tasks are **never** highlighted as "overdue."
*   They are moved quietly into the **Archives** to protect the Commander from guilt.
*   If a task is recurring, a fresh copy is generated for the new day. The Commander begins again at dawn with a clean slate.
