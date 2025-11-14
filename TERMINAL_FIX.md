# 🛠️ Terminal Input Issue Fix

## Problem Description

Windows PowerShell can get stuck in "paste mode" or send control characters (like `^B`, `^C`) instead of plain text. This prevents the StockCast TCP client from receiving proper keyboard input.

## Why This Happens

- **Middle-click paste** triggers binary input mode
- **Ctrl + key combinations** send control codes instead of characters
- **Terminal selection mode** freezes input
- **Unicode/encoding issues** insert invisible characters
- **PowerShell ISE quirks** with stdin

---

## ✅ Solutions Applied

### 1. **Enhanced Java Client** (`StockCastClient.java`)

- Added `stripControlCharacters()` method to remove control codes
- Implemented `System.console()` with fallback to `BufferedReader`
- Proper UTF-8 encoding handling
- Removes characters in ranges: `0x00-0x1F` (except tab/newline)

### 2. **Fixed PowerShell Launcher** (`run-client.ps1`)

- Resets console input mode: `[Console]::TreatControlCAsInput = $false`
- Sets UTF-8 encoding for input/output
- Clears stuck input buffer before starting
- Sets Java encoding: `-Dfile.encoding=UTF-8`

---

## 🚀 How to Use

### Method 1: Use the Fixed Launcher (Recommended)

```powershell
.\run-client.ps1
```

This automatically configures the terminal correctly.

### Method 2: Manual Terminal Reset

If you're already stuck in control character mode:

1. **Close the current terminal completely** (don't just clear it)
2. **Open a fresh PowerShell window**
3. Run these commands before starting the client:

```powershell
[Console]::TreatControlCAsInput = $false
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Clear stuck input
while ([Console]::KeyAvailable) {
    [Console]::ReadKey($true) | Out-Null
}
```

4. Then run the client:

```powershell
.\run-client.ps1
```

### Method 3: Use Windows Terminal (Best)

Windows Terminal handles input better than PowerShell:

1. Install **Windows Terminal** from Microsoft Store
2. Open Windows Terminal
3. Run the client:

```powershell
.\run-client.ps1
```

### Method 4: Use Command Prompt (cmd.exe)

If PowerShell continues to have issues:

1. Open **Command Prompt** (cmd.exe)
2. Navigate to the project:

```cmd
cd D:\StockCast
```

3. Run:

```cmd
powershell -File run-client.ps1
```

---

## 🔍 Detecting the Issue

### Symptoms:

- Typing shows `^B`, `^C`, `^D` instead of letters
- No command is sent when you press Enter
- Copy/paste doesn't work
- Server shows no activity from client

### Test:

In your terminal, type:

```
echo hello
```

If you see `^H^E^L^L^O` or similar → **Terminal is stuck**

---

## 🔧 Emergency Fixes

### If Still Stuck After Fixes:

#### 1. **Kill All Java Processes**

```powershell
Get-Process java | Stop-Process -Force
```

#### 2. **Reset PowerShell Profile**

```powershell
# Temporarily disable profile
powershell -NoProfile
```

#### 3. **Use Java Console Directly**

Skip PowerShell entirely:

```cmd
cd backend
mvn compile
java -Dfile.encoding=UTF-8 -cp target/classes com.stockcast.client.StockCastClient localhost 9090
```

#### 4. **Check for Background Processes**

```powershell
# See if something is reading your terminal input
Get-Process | Where-Object {$_.MainWindowTitle -like "*PowerShell*"}
```

---

## 📝 Testing the Fix

After applying the fix, test that input works:

1. Start the server:

```powershell
.\run-server.ps1
```

2. In a **new terminal**, start the client:

```powershell
.\run-client.ps1
```

3. Type commands (should work normally now):

```
subscribe AAPL
list
ping
```

You should see:

- ✓ Normal text input (no `^B` characters)
- ✓ Commands execute immediately
- ✓ Server responds with acknowledgments

---

## 🐛 If Problem Persists

### Check Java Console Mode:

```java
// Add this to StockCastClient.java main() for debugging:
System.out.println("Console available: " + (System.console() != null));
System.out.println("Input encoding: " + System.getProperty("file.encoding"));
System.out.println("OS: " + System.getProperty("os.name"));
```

### Run with Debug Output:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Djava.util.logging.config.file=logging.properties"
.\run-client.ps1
```

### Use Alternative Terminal:

- **Git Bash** (bundled with Git for Windows)
- **WSL** (Windows Subsystem for Linux)
- **ConEmu** or **Cmder**

---

## 📚 Additional Resources

- [Microsoft: PowerShell Console Input](https://docs.microsoft.com/en-us/powershell/scripting/windows-powershell/ise/how-to-use-the-console-pane-in-the-windows-powershell-ise)
- [Java System.console() Documentation](https://docs.oracle.com/javase/8/docs/api/java/io/Console.html)
- [Windows Terminal on GitHub](https://github.com/microsoft/terminal)

---

## ✅ Summary

The fix involves:

1. **Java side**: Strip control characters and use proper console handling
2. **PowerShell side**: Reset terminal mode and set UTF-8 encoding
3. **User side**: Use fresh terminal or Windows Terminal

**The updated `run-client.ps1` script now handles this automatically!**
