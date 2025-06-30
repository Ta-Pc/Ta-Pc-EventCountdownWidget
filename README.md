# Calender Event Countdown Widget

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Language](https://img.shields.io/badge/language-Java-purple.svg)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)
![UI](https://img.shields.io/badge/UI-Material%20You-blue.svg)

A sleek, modern, and highly customizable home screen widget for Android that provides at-a-glance countdowns to your important calendar events. Designed with a focus on clarity, aesthetics, and user experience, this app helps you anticipate and prepare for what's next.

---

### ✨ App in Action

<p align="center">
  <img src="https://github.com/user-attachments/assets/a2177c3f-a460-4d56-aa38-970db104705a" alt="Homescreen w/ widgets" width="30%"/>
  <img src="https://github.com/user-attachments/assets/b6216560-a1db-4242-8fdf-ed6608b56086" alt="Homescreen w/ widgets" width="30%"/>
  <img src="https://github.com/user-attachments/assets/d0aa9bc8-d761-400f-9c06-1eeed82f0fee" alt="Launcher" width="30%"/>
  <img src="https://github.com/user-attachments/assets/731ad3be-ca20-4894-8df4-f241ba7f1931" alt="Event List Widget" width="30%"/>
  <img src="https://github.com/user-attachments/assets/f7362982-e404-4d4a-a6e2-0df319807a83" alt="Event List Widget" width="30%"/>
  <img src="https://github.com/user-attachments/assets/affbdbe2-070a-4ad6-91a6-91b35024dda5" alt="Configuration Screen" width="30%"/>
</p>

---

### 🚀 Key Features

| Feature                          | Description                                                                                                                                                    |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 🗓️ **Two Widget Types**           | Choose between a **Single Event** widget for focused countdowns or a scrollable **Event List** widget to see multiple upcoming events at a glance.                |
| 🎨 **Dynamic & Customizable Theming** | Personalize your widgets with a beautiful color palette, manual Light/Dark modes, and full **Material You** support for automatic wallpaper-based theming on Android 12+. |
| 🧠 **Intelligent Countdown Display** | The Single Event widget shows a detailed breakdown (years, months, days, hours), while the List widget uses concise summaries ("Now", "Ended", "Tomorrow") for easy scanning. |
| 🔗 **Direct Calendar Integration**  | Tap an event to open it directly in your calendar app. Fine-tune which specific calendars (e.g., Personal, Work) are used as a data source.                    |
| 🔄 **Reliable & Efficient Updates** | Widgets update automatically every minute using an efficient `AlarmManager` implementation that correctly resumes after a device reboot.                     |
| ⚙️ **User-Friendly Configuration** | Simple and intuitive configuration screens for each widget type allow you to tailor its appearance, data source, and event limits with ease.                 |

---

### 🎨 Design Philosophy

The design is heavily inspired by **Swiss Style** graphic design principles, emphasizing:
*   **Clarity & Readability:** A clean, legible sans-serif font with a strong information hierarchy.
*   **High Contrast:** Text color is carefully chosen to ensure legibility in all themes.
*   **Minimalism:** A flat, modern aesthetic with uniformly rounded corners and no unnecessary visual clutter. The focus is on the content itself.
*   **Consistency:** A cohesive design language across all widgets and configuration screens.

---

### 🛠️ Technical Overview

This application is a practical demonstration of modern Android widget development, focusing on a clean architecture and best practices.

<details>
  <summary><strong>Click to see Architecture and Key Components</strong></summary>

  #### Architecture
  *   **AppWidgetProvider:** The core logic for each widget (`EventCountdownWidget`, `SimpleEventListWidgetProvider`) is managed by its own provider, handling widget lifecycle events.
  *   **Configuration Activities:** Dedicated `Activity` classes (`WidgetConfigActivity`, `SimpleListWidgetConfigActivity`) handle user setup for each widget instance.
  *   **RemoteViewsService:** The scrollable `ListView` in the Event List widget is powered by a `RemoteViewsService` (`EventListWidgetService`), which is the standard, performant way to display collections in widgets.
  *   **Separation of Concerns:** Helper classes like `CalendarRepository`, `CountdownFormatter`, and `ThemeManager` are used to create a modular and maintainable codebase.

  #### Key Android Components & APIs
  *   **`AppWidgetManager`:** Manages all aspects of widget creation, updates, and deletion.
  *   **`RemoteViews`:** Builds the widget UI that runs in the home screen launcher process.
  *   **`AlarmManager` & `BroadcastReceiver`:** A robust system for scheduling reliable, minute-by-minute automatic updates (`WidgetUpdateReceiver`) and ensuring they resume after a device reboot (`BootReceiver`).
  *   **`CalendarContract.Instances`:** The primary Android API for efficiently querying calendar event instances within a specific time range.
  *   **`SharedPreferences`:** Persists user configuration for each unique widget instance.
  *   **Material Components:** Used for creating the modern, consistent UI in the configuration activities.

</details>

---

### ⚙️ Getting Started

To build and run this project, you will need the latest stable version of Android Studio.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/YourUsername/Your-Repo-Name.git
    ```
2.  **Open in Android Studio:**
    *   Open Android Studio.
    *   Select `File -> Open` and navigate to the cloned project directory.
3.  **Sync & Run:**
    *   Allow Gradle to sync and download the necessary dependencies.
    *   Select your target device (emulator or physical device) and click the "Run" button.

### 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
