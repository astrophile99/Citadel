# Citadel Architecture Guidelines

## Technical Stack & Patterns
We align with modern Android guidelines, enforcing robust, clean, and highly testable architectural layers:

```
+-------------------------------------------------------+
| UI Layer (Jetpack Compose, Composable Screens)       |
+-------------------------------------------------------+
                           |  (Observes StateFlow UI state)
                           v
+-------------------------------------------------------+
| ViewModel Layer (MVVM architecture, UI State Holders) |
+-------------------------------------------------------+
                           |  (Calls business logic)
                           v
+-------------------------------------------------------+
| Repository Layer (Data coordination, single source)   |
+-------------------------------------------------------+
                           |
            +--------------+--------------+
            |                             |
            v                             v
+-----------------------+     +-----------------------+
| Local DB (Room/SQLite)|     | System / Preferences  |
+-----------------------+     +-----------------------+
```

---

## 1. The Single Source of Truth
*   **The Repository**: The UI must never write directly to Database or SharedPreferences. All operations are channeled through repositories.
*   **UI State Flows**: ViewModels must expose a single read-only `StateFlow<UiState>` containing the exact data representation needed for the Composable screen.

---

## 2. Jetpack Compose Performance Rules
*   **Immutable States**: State objects in StateFlow should be kotlin `data class` structures where all properties are immutable (`val`).
*   **Avoid Composure Overhead**: 
    *   Do not perform heavy logic, date calculation, or text formatting directly inside `@Composable` functions. All calculations must occur in the ViewModel or use Compose `remember` blocks.
    *   Use `derivedStateOf` when drawing UI elements dependent on fast-changing values (like scrolling positions or timer ticks).

---

## 3. Separation of Theme and Code
*   **No Hardcoded Colors**: Colors must always reference the current context theme, e.g., `MaterialTheme.colorScheme.primary` or `MaterialTheme.colorScheme.background`, never hardcoded hex codes.
*   **Scale Independence**: All sizes must be defined in `dp` or `sp` (for text size) to ensure smooth scaling across phones, foldables, and tablets.
