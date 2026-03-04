# Optoma Serial Remote

A remote control solution for Optoma projectors via the RS232 (Serial) interface. This project was specifically tested on the **Optoma EX556**, but it should be compatible with most Optoma projectors that support the standard serial protocol.

> **Note:** This project primarily consists of AI-generated code. Some bugs may exist.

---

## 💻 Python Version
Located in: `python_remote/rs232_remote.py`

* **Platform Support:** Primarily designed for Windows (uses Windows-specific keycode mapping). Minor adjustments are required for macOS or Linux.
* **Installation:** * Pre-built executables are available in the [Releases](#) page (compiled with PyInstaller on Python 3.10 / Windows 11).
    * To build from source, use tools like: `pyinstaller` and run your build script.
* **Key Features:** Supports both GUI button clicks and physical keyboard shortcuts.

## 📱 Android Version
The Android client allows you to control the projector using a mobile device and a USB-to-RS232 dongle.

* **Compatibility:** Android 6.0 (Marshmallow) and newer.
* **Plug & Play:** Once connected via a USB OTG adapter, a system dialog will prompt for permission to launch the app automatically.
* **Build Environment:** Developed using Android Studio 2025.3.1 Patch 1.
* **Installation:** * Pre-built executables are available in the [Releases](#) page.

---

## 🛠 Customizing Keymaps

You can extend the functionality by adding more commands found in your projector's manual.

### 1. Android Configuration
Modify the `commands` array in the Java source code:
```java
// Structure: {Display Name, Serial Command, Android KeyEvent}
Object[][] commands = {
    {"", "", null}, // Empty space for layout
    {"↑", "00140 10", KeyEvent.KEYCODE_DPAD_UP},
    {"", "", null},
    {"←", "00140 11", KeyEvent.KEYCODE_DPAD_LEFT},
    {"Enter", "00140 12", KeyEvent.KEYCODE_ENTER},
    {"→", "00140 13", KeyEvent.KEYCODE_DPAD_RIGHT},
    // ... add more keys here
};

```
* **Layout:** The keypad is rendered in a **3-column grid**. Use `{"", "", null}` to create empty spacers (e.g., for arrow key alignment).
* **Serial Command:** The string code sent to the projector.
* **Android Keycode:** Used for triggering actions via an external physical keyboard.

### 2. Python Configuration

Modify the `commands` list in `rs232_remote.py`:

```python
# Structure: (Display Name, Serial Command, Keycode/Char)
commands = [
    ("", "", None), ("↑", "00140 10", 38), ("", "", None),
    ("←", "00140 11", 37), ("Enter", "00140 12", 13), ("→", "00140 13", 39),
    # ...
]

```

* **Layout:** The keypad is rendered in a **3-column grid**. Use `("", "", None)` to create empty and disabled button as the spacer (e.g., for arrow key alignment).
* **Serial Command:** The string code sent to the projector.
* **Keycode/Char:** Used for triggering actions via keyboard, alphanumeric key can use the char like "a" directly.

If you add a special key (non-alphanumeric), you must also update the `KEYCODE_MAP`:

```python
# Mapping for keycode and key name
KEYCODE_MAP = {
    37: 'Left', 38: 'Up', 39: 'Right', 40: 'Down',
    13: 'Enter', 188: ',',
}

```

## 🖼 Gallery
![Demo Picture](pictures/demo_1.jpg)
![Android Preview](pictures/android_1.jpg)
![Windows Preview](pictures/windows_1.png)