package com.nexus.system;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cross-Platform Application Launcher Service for Project N.E.X.U.S.
 * Detects the host operating system (Windows / macOS / Linux) and launches
 * desktop applications via OS-native shell commands.
 *
 * Supports:
 * - Windows: cmd /c start, direct .exe paths, Start-Process
 * - macOS: open -a "AppName", direct .app paths
 * - Linux: xdg-open, direct binary names
 * - Cross-platform website/URL opening in default browser
 *
 * Includes a built-in catalog of ~40 common applications and ~20 popular
 * websites with their platform-specific identifiers for instant lookup.
 */
public class AppLauncherService {

    public enum Platform {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    /**
     * Result of an app launch attempt.
     */
    public static class LaunchResult {
        private final boolean success;
        private final String appName;
        private final String message;

        public LaunchResult(boolean success, String appName, String message) {
            this.success = success;
            this.appName = appName;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getAppName() { return appName; }
        public String getMessage() { return message; }
    }

    /**
     * Represents a known application with platform-specific launch identifiers.
     */
    private static class AppEntry {
        final String displayName;
        final String[] aliases;         // fuzzy-match keywords
        final String windowsCmd;        // Windows launch command or exe name
        final String macCmd;            // macOS app name for "open -a"
        final String linuxCmd;          // Linux binary name

        AppEntry(String displayName, String[] aliases, String windowsCmd, String macCmd, String linuxCmd) {
            this.displayName = displayName;
            this.aliases = aliases;
            this.windowsCmd = windowsCmd;
            this.macCmd = macCmd;
            this.linuxCmd = linuxCmd;
        }
    }

    /**
     * Represents a known website that can be opened in the default browser.
     */
    private static class WebsiteEntry {
        final String displayName;
        final String[] aliases;
        final String url;

        WebsiteEntry(String displayName, String[] aliases, String url) {
            this.displayName = displayName;
            this.aliases = aliases;
            this.url = url;
        }
    }

    private static final Platform CURRENT_PLATFORM;
    private static final List<AppEntry> APP_CATALOG = new ArrayList<>();
    private static final List<WebsiteEntry> WEBSITE_CATALOG = new ArrayList<>();

    static {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            CURRENT_PLATFORM = Platform.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            CURRENT_PLATFORM = Platform.MACOS;
        } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            CURRENT_PLATFORM = Platform.LINUX;
        } else {
            CURRENT_PLATFORM = Platform.UNKNOWN;
        }

        // ─────────────── APPLICATION CATALOG ───────────────
        // Browsers
        registerApp("Google Chrome",
                new String[]{"chrome", "google chrome", "browser"},
                "chrome", "Google Chrome", "google-chrome");

        registerApp("Microsoft Edge",
                new String[]{"edge", "microsoft edge"},
                "msedge", "Microsoft Edge", "microsoft-edge");

        registerApp("Mozilla Firefox",
                new String[]{"firefox", "mozilla", "mozilla firefox"},
                "firefox", "Firefox", "firefox");

        registerApp("Opera",
                new String[]{"opera", "opera browser"},
                "opera", "Opera", "opera");

        registerApp("Safari",
                new String[]{"safari"},
                null, "Safari", null);

        registerApp("Brave Browser",
                new String[]{"brave", "brave browser"},
                "brave", "Brave Browser", "brave-browser");

        // Productivity / Office
        registerApp("Microsoft Word",
                new String[]{"word", "ms word", "microsoft word"},
                "winword", "Microsoft Word", "libreoffice --writer");

        registerApp("Microsoft Excel",
                new String[]{"excel", "ms excel", "microsoft excel", "spreadsheet"},
                "excel", "Microsoft Excel", "libreoffice --calc");

        registerApp("Microsoft PowerPoint",
                new String[]{"powerpoint", "ppt", "ms powerpoint", "presentation"},
                "powerpnt", "Microsoft PowerPoint", "libreoffice --impress");

        registerApp("Microsoft Outlook",
                new String[]{"outlook", "ms outlook", "email", "mail"},
                "outlook", "Microsoft Outlook", "thunderbird");

        registerApp("Microsoft Teams",
                new String[]{"teams", "ms teams", "microsoft teams"},
                "ms-teams", "Microsoft Teams", "teams");

        registerApp("Notepad",
                new String[]{"notepad", "text editor", "notes"},
                "notepad", "TextEdit", "gedit");

        registerApp("Calculator",
                new String[]{"calculator", "calc", "calculation", "math"},
                "calc", "Calculator", "gnome-calculator");

        registerApp("File Explorer",
                new String[]{"file explorer", "explorer", "files", "file manager", "my computer", "folders", "my files", "this pc"},
                "explorer", "Finder", "nautilus");

        registerApp("Command Prompt",
                new String[]{"cmd", "command prompt", "terminal", "command line", "console", "shell"},
                "cmd", "Terminal", "gnome-terminal");

        registerApp("PowerShell",
                new String[]{"powershell", "ps"},
                "powershell", "Terminal", "bash");

        // Development Tools
        registerApp("Visual Studio Code",
                new String[]{"vscode", "vs code", "visual studio code", "code editor"},
                "code", "Visual Studio Code", "code");

        registerApp("IntelliJ IDEA",
                new String[]{"intellij", "idea", "intellij idea"},
                "idea64", "IntelliJ IDEA", "idea");

        registerApp("Android Studio",
                new String[]{"android studio"},
                "studio64", "Android Studio", "android-studio");

        registerApp("Git Bash",
                new String[]{"git bash", "git"},
                "git-bash", "Terminal", "bash");

        // Communication
        registerApp("Discord",
                new String[]{"discord"},
                "discord", "Discord", "discord");

        registerApp("Slack",
                new String[]{"slack"},
                "slack", "Slack", "slack");

        registerApp("Telegram",
                new String[]{"telegram"},
                "telegram", "Telegram", "telegram-desktop");

        registerApp("WhatsApp",
                new String[]{"whatsapp"},
                "whatsapp", "WhatsApp", "whatsapp-desktop");

        registerApp("Zoom",
                new String[]{"zoom", "zoom meeting"},
                "zoom", "zoom.us", "zoom");

        registerApp("Skype",
                new String[]{"skype"},
                "skype", "Skype", "skype");

        // Media
        registerApp("Spotify",
                new String[]{"spotify", "music"},
                "spotify", "Spotify", "spotify");

        registerApp("VLC Media Player",
                new String[]{"vlc", "vlc player", "media player", "video player"},
                "vlc", "VLC", "vlc");

        registerApp("Windows Media Player",
                new String[]{"windows media player", "wmp"},
                "wmplayer", null, null);

        registerApp("Photos",
                new String[]{"photos", "photo viewer"},
                "ms-photos:", "Photos", "eog");

        // System Utilities
        registerApp("Settings",
                new String[]{"settings", "system settings", "preferences", "control panel"},
                "ms-settings:", "System Preferences", "gnome-control-center");

        registerApp("Task Manager",
                new String[]{"task manager", "taskmgr", "processes"},
                "taskmgr", "Activity Monitor", "gnome-system-monitor");

        registerApp("Paint",
                new String[]{"paint", "mspaint", "drawing"},
                "mspaint", "Preview", "gimp");

        registerApp("Snipping Tool",
                new String[]{"snipping tool", "screenshot", "snip", "screen capture"},
                "snippingtool", "Screenshot", "gnome-screenshot");

        registerApp("Clock",
                new String[]{"clock", "alarm", "timer", "stopwatch"},
                "ms-clock:", "Clock", null);

        registerApp("Maps",
                new String[]{"maps", "google maps"},
                "bingmaps:", "Maps", "gnome-maps");

        registerApp("Camera",
                new String[]{"camera", "webcam"},
                "microsoft.windows.camera:", "FaceTime", "cheese");

        registerApp("Store",
                new String[]{"store", "app store", "microsoft store"},
                "ms-windows-store:", "App Store", "snap-store");

        // Creative & Design
        registerApp("Adobe Photoshop",
                new String[]{"photoshop", "adobe photoshop"},
                "photoshop", "Adobe Photoshop 2025", "gimp");

        registerApp("Figma",
                new String[]{"figma"},
                "figma", "Figma", "figma");

        registerApp("Blender",
                new String[]{"blender", "3d"},
                "blender", "Blender", "blender");

        // ─────────────── WEBSITE CATALOG ───────────────
        registerWebsite("YouTube",
                new String[]{"youtube", "yt", "you tube", "youtub"},
                "https://www.youtube.com");

        registerWebsite("Google",
                new String[]{"google", "google search", "search"},
                "https://www.google.com");

        registerWebsite("Gmail",
                new String[]{"gmail", "google mail", "email", "my email", "my mail"},
                "https://mail.google.com");

        registerWebsite("Google Drive",
                new String[]{"google drive", "drive", "gdrive"},
                "https://drive.google.com");

        registerWebsite("Google Docs",
                new String[]{"google docs", "docs", "gdocs"},
                "https://docs.google.com");

        registerWebsite("Google Classroom",
                new String[]{"google classroom", "classroom"},
                "https://classroom.google.com");

        registerWebsite("GitHub",
                new String[]{"github", "git hub"},
                "https://github.com");

        registerWebsite("ChatGPT",
                new String[]{"chatgpt", "chat gpt", "openai"},
                "https://chat.openai.com");

        registerWebsite("WhatsApp Web",
                new String[]{"whatsapp web", "wa web"},
                "https://web.whatsapp.com");

        registerWebsite("Instagram",
                new String[]{"instagram", "insta"},
                "https://www.instagram.com");

        registerWebsite("Twitter / X",
                new String[]{"twitter", "x", "x.com"},
                "https://x.com");

        registerWebsite("Facebook",
                new String[]{"facebook", "fb"},
                "https://www.facebook.com");

        registerWebsite("LinkedIn",
                new String[]{"linkedin", "linked in"},
                "https://www.linkedin.com");

        registerWebsite("Reddit",
                new String[]{"reddit"},
                "https://www.reddit.com");

        registerWebsite("Netflix",
                new String[]{"netflix"},
                "https://www.netflix.com");

        registerWebsite("Amazon",
                new String[]{"amazon", "amazon shopping"},
                "https://www.amazon.com");

        registerWebsite("Wikipedia",
                new String[]{"wikipedia", "wiki"},
                "https://www.wikipedia.org");

        registerWebsite("Stack Overflow",
                new String[]{"stackoverflow", "stack overflow"},
                "https://stackoverflow.com");

        registerWebsite("Canva",
                new String[]{"canva"},
                "https://www.canva.com");

        registerWebsite("Google Maps",
                new String[]{"google maps", "gmaps"},
                "https://maps.google.com");
    }

    private static void registerApp(String displayName, String[] aliases, String windowsCmd, String macCmd, String linuxCmd) {
        APP_CATALOG.add(new AppEntry(displayName, aliases, windowsCmd, macCmd, linuxCmd));
    }

    private static void registerWebsite(String displayName, String[] aliases, String url) {
        WEBSITE_CATALOG.add(new WebsiteEntry(displayName, aliases, url));
    }

    /**
     * Returns the detected platform.
     */
    public static Platform getCurrentPlatform() {
        return CURRENT_PLATFORM;
    }

    /**
     * Returns the platform name as a user-friendly string.
     */
    public static String getPlatformDisplayName() {
        return switch (CURRENT_PLATFORM) {
            case WINDOWS -> "Windows";
            case MACOS -> "macOS";
            case LINUX -> "Linux";
            default -> "Unknown OS";
        };
    }

    /**
     * Attempts to launch an application by name. Uses fuzzy matching against the catalog.
     * If no catalog match is found, attempts a direct OS-level launch.
     *
     * @param appQuery The user's app name query (e.g. "chrome", "calculator", "open settings")
     * @return LaunchResult with success/failure info
     */
    public LaunchResult launchApp(String appQuery) {
        if (appQuery == null || appQuery.isBlank()) {
            return new LaunchResult(false, "", "No application name provided.");
        }

        String normalizedQuery = normalizeQuery(appQuery);
        System.out.println("[AppLauncher] Launch request: \"" + appQuery + "\" → normalized: \"" + normalizedQuery + "\" on " + CURRENT_PLATFORM);

        // 1. Try website catalog lookup first
        WebsiteEntry websiteMatch = findBestWebsiteMatch(normalizedQuery);
        if (websiteMatch != null) {
            return openWebsite(websiteMatch);
        }

        // 2. Check if it looks like a raw URL
        if (looksLikeUrl(appQuery)) {
            return openUrl(appQuery, appQuery);
        }

        // 3. Try app catalog lookup (fuzzy match)
        AppEntry bestMatch = findBestMatch(normalizedQuery);
        if (bestMatch != null) {
            return launchCatalogApp(bestMatch);
        }

        // 4. Try direct OS-level launch with the raw query
        return launchDirect(normalizedQuery, appQuery);
    }

    /**
     * Asynchronously launches an application.
     */
    public CompletableFuture<LaunchResult> launchAppAsync(String appQuery) {
        return CompletableFuture.supplyAsync(() -> launchApp(appQuery));
    }

    /**
     * Returns a list of all known application names from the catalog.
     */
    public List<String> getAvailableApps() {
        List<String> names = new ArrayList<>();
        for (AppEntry entry : APP_CATALOG) {
            names.add(entry.displayName);
        }
        return names;
    }

    /**
     * Checks whether the user's input text contains an app-launch intent.
     * Returns the extracted app name, or null if no intent detected.
     */
    public static String extractAppLaunchIntent(String userText) {
        if (userText == null) return null;
        String raw = userText.trim();
        String lower = raw.toLowerCase()
                .replaceAll("^[\\s,;.!?-]+", "")
                .replaceAll("[\\s,;.!?-]+$", "");

        if (lower.isEmpty()) return null;

        // Prefix patterns for natural language commands (longest first)
        String[] prefixes = {
            "could you please open up ", "could you please launch ", "could you please start ", "could you please open ",
            "can you please open up ", "can you please launch ", "can you please start ", "can you please open ",
            "can u please open up ", "can u please launch ", "can u please start ", "can u please open ",
            "could you open up ", "could you launch ", "could you start ", "could you open ",
            "could u open up ", "could u launch ", "could u start ", "could u open ",
            "can you open up ", "can you launch ", "can you start ", "can you open ",
            "can u open up ", "can u launch ", "can u start ", "can u open ",
            "would you open up ", "would you launch ", "would you open ",
            "would u open up ", "would u launch ", "would u open ",
            "please open up ", "please launch ", "please start ", "please open ",
            "u can open ", "you can open ",
            "open up ", "fire up ", "boot up ", "bring up ",
            "open the ", "launch the ", "start the ", "run the ",
            "open my ", "launch my ", "start my ",
            "navigate to ", "browse to ", "take me to ", "go to ", "visit ",
            "open ", "launch ", "start ", "run ", "play "
        };

        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String appName = lower.substring(prefix.length()).trim();
                // Clean trailing fill words
                appName = appName.replaceAll("\\s+(app|application|software|program|website|site|page|tab|for me|please|pls|plz|now|immediately)$", "").trim();
                appName = appName.replaceAll("[.!?]+$", "").trim();
                if (!appName.isEmpty()) {
                    return appName;
                }
            }
        }

        // Direct app/website mention (e.g. user simply typed "youtube", "chrome", "calc", "calculator", "files", "explorer", "spotify", "notepad")
        if (isCatalogAppOrWebsite(lower)) {
            return lower;
        }

        // Direct URL entered
        if (looksLikeUrl(lower)) {
            return lower;
        }

        return null;
    }

    /**
     * Checks if the given string directly matches any known app or website in the catalog.
     */
    public static boolean isCatalogAppOrWebsite(String query) {
        if (query == null || query.isBlank()) return false;
        String normalized = query.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();

        for (WebsiteEntry w : WEBSITE_CATALOG) {
            if (w.displayName.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim().equals(normalized)) return true;
            for (String a : w.aliases) {
                if (a.equals(normalized)) return true;
            }
        }
        for (AppEntry a : APP_CATALOG) {
            if (a.displayName.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim().equals(normalized)) return true;
            for (String al : a.aliases) {
                if (al.equals(normalized)) return true;
            }
        }
        return false;
    }

    // ────────────── Internal Methods ──────────────

    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

    private AppEntry findBestMatch(String normalizedQuery) {
        AppEntry exactMatch = null;
        AppEntry partialMatch = null;
        int bestPartialScore = 0;

        for (AppEntry entry : APP_CATALOG) {
            // Check aliases
            for (String alias : entry.aliases) {
                if (alias.equals(normalizedQuery)) {
                    return entry; // Exact alias match
                }
                if (normalizedQuery.contains(alias) || alias.contains(normalizedQuery)) {
                    int score = Math.min(alias.length(), normalizedQuery.length());
                    if (score > bestPartialScore) {
                        bestPartialScore = score;
                        partialMatch = entry;
                    }
                }
            }

            // Check display name
            if (entry.displayName.toLowerCase().equals(normalizedQuery)) {
                exactMatch = entry;
            }
        }

        return (exactMatch != null) ? exactMatch : partialMatch;
    }

    private LaunchResult launchCatalogApp(AppEntry entry) {
        String cmd = switch (CURRENT_PLATFORM) {
            case WINDOWS -> entry.windowsCmd;
            case MACOS -> entry.macCmd;
            case LINUX -> entry.linuxCmd;
            default -> null;
        };

        if (cmd == null || cmd.isEmpty()) {
            return new LaunchResult(false, entry.displayName,
                    entry.displayName + " is not available on " + getPlatformDisplayName() + ".");
        }

        System.out.println("[AppLauncher] Launching catalog app: " + entry.displayName + " via command: " + cmd);

        if (CURRENT_PLATFORM == Platform.WINDOWS) {
            // ── Windows Execution Strategies ──

            // Strategy 1: Special case: File Explorer
            if (cmd.equalsIgnoreCase("explorer")) {
                try {
                    new ProcessBuilder("explorer.exe").start();
                    System.out.println("[AppLauncher] ✓ Launched File Explorer directly via explorer.exe");
                    return new LaunchResult(true, entry.displayName,
                            "File Explorer has been opened successfully on Windows.");
                } catch (Exception e) {
                    try {
                        new ProcessBuilder("powershell", "-NoProfile", "-Command", "Start-Process explorer").start();
                        return new LaunchResult(true, entry.displayName,
                                "File Explorer has been opened successfully on Windows.");
                    } catch (Exception ignored) {}
                }
            }

            // Strategy 2: Protocol URIs (e.g. ms-settings:, ms-photos:, calc:, bingmaps:, microsoft.windows.camera:)
            if (cmd.contains(":") && !cmd.contains("\\") && !cmd.contains("/")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "Start-Process '" + cmd + "'");
                    pb.redirectErrorStream(true);
                    pb.start();
                    System.out.println("[AppLauncher] ✓ Launched protocol URI: " + cmd);
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Windows.");
                } catch (Exception e) {
                    try {
                        new ProcessBuilder("cmd.exe", "/c", "start", cmd).start();
                        return new LaunchResult(true, entry.displayName,
                                entry.displayName + " has been launched successfully on Windows.");
                    } catch (Exception ignored) {}
                }
            }

            // Strategy 3: PowerShell Start-Process (resolves Windows App Paths, PATH, Store apps)
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "Start-Process '" + cmd + "'");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(3, TimeUnit.SECONDS);
                if (!finished || process.exitValue() == 0) {
                    System.out.println("[AppLauncher] ✓ Launched " + entry.displayName + " via PowerShell Start-Process");
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Windows.");
                }
            } catch (Exception e) {
                System.err.println("[AppLauncher] PowerShell Start-Process failed: " + e.getMessage());
            }

            // Strategy 4: Direct executable launch (e.g. calc.exe, notepad.exe, mspaint.exe)
            try {
                String exeName = cmd.endsWith(".exe") ? cmd : (cmd + ".exe");
                ProcessBuilder pb = new ProcessBuilder(exeName);
                pb.redirectErrorStream(true);
                pb.directory(new File(System.getProperty("user.home")));
                Process process = pb.start();
                boolean exited = process.waitFor(2, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    System.out.println("[AppLauncher] ✓ Direct launch of " + exeName + " succeeded");
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Windows.");
                }
            } catch (Exception ignored) {}

            // Strategy 5: Windows cmd.exe /c start "" <cmd>
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "", cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean exited = process.waitFor(2, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    System.out.println("[AppLauncher] ✓ Launched " + entry.displayName + " via cmd.exe start");
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Windows.");
                }
            } catch (Exception ignored) {}

            // Strategy 6: Check common install locations for known apps
            LaunchResult knownResult = tryKnownInstallPaths(entry);
            if (knownResult != null && knownResult.isSuccess()) {
                return knownResult;
            }

            return new LaunchResult(false, entry.displayName,
                    "Could not launch " + entry.displayName + ". Please ensure it is installed on your Windows PC.");

        } else if (CURRENT_PLATFORM == Platform.MACOS) {
            // ── macOS Execution Strategies ──
            try {
                ProcessBuilder pb = new ProcessBuilder("open", "-a", entry.macCmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    System.out.println("[AppLauncher] ✓ Launched " + entry.displayName + " on macOS via open -a");
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on macOS.");
                }
            } catch (Exception e) {
                System.err.println("[AppLauncher] macOS open -a failed: " + e.getMessage());
            }

            // Fallback: try opening /Applications/<macCmd>.app directly
            try {
                File appFile = new File("/Applications/" + entry.macCmd + ".app");
                if (appFile.exists()) {
                    new ProcessBuilder("open", appFile.getAbsolutePath()).start();
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on macOS.");
                }
            } catch (Exception ignored) {}

            return new LaunchResult(false, entry.displayName,
                    "Could not launch " + entry.displayName + " on macOS. Please ensure it is installed in /Applications.");

        } else {
            // ── Linux Execution Strategies ──
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean exited = process.waitFor(2, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Linux.");
                }
            } catch (Exception e) {
                System.err.println("[AppLauncher] Linux launch failed: " + e.getMessage());
            }

            return new LaunchResult(false, entry.displayName,
                    "Could not launch " + entry.displayName + " on Linux.");
        }
    }

    private LaunchResult tryKnownInstallPaths(AppEntry entry) {
        String name = entry.displayName.toLowerCase();
        List<String> candidatePaths = new ArrayList<>();

        String localApp = System.getenv("LOCALAPPDATA");
        String appData = System.getenv("APPDATA");
        String progFiles = System.getenv("ProgramFiles");
        String progFilesX86 = System.getenv("ProgramFiles(x86)");

        if (name.contains("chrome")) {
            if (progFiles != null) candidatePaths.add(progFiles + "\\Google\\Chrome\\Application\\chrome.exe");
            if (progFilesX86 != null) candidatePaths.add(progFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
            if (localApp != null) candidatePaths.add(localApp + "\\Google\\Chrome\\Application\\chrome.exe");
        } else if (name.contains("edge")) {
            if (progFilesX86 != null) candidatePaths.add(progFilesX86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
            if (progFiles != null) candidatePaths.add(progFiles + "\\Microsoft\\Edge\\Application\\msedge.exe");
        } else if (name.contains("code") || name.contains("vs code")) {
            if (localApp != null) candidatePaths.add(localApp + "\\Programs\\Microsoft VS Code\\Code.exe");
            if (progFiles != null) candidatePaths.add(progFiles + "\\Microsoft VS Code\\Code.exe");
        } else if (name.contains("spotify")) {
            if (appData != null) candidatePaths.add(appData + "\\Spotify\\Spotify.exe");
        } else if (name.contains("discord")) {
            if (localApp != null) candidatePaths.add(localApp + "\\Discord\\Update.exe --processStart Discord.exe");
        } else if (name.contains("notepad")) {
            candidatePaths.add("C:\\Windows\\notepad.exe");
            candidatePaths.add("C:\\Windows\\System32\\notepad.exe");
        } else if (name.contains("calculator")) {
            candidatePaths.add("C:\\Windows\\System32\\calc.exe");
        }

        for (String path : candidatePaths) {
            File f = new File(path.split(" ")[0]);
            if (f.exists()) {
                try {
                    new ProcessBuilder(path.split(" ")).start();
                    System.out.println("[AppLauncher] ✓ Launched " + entry.displayName + " from absolute path: " + path);
                    return new LaunchResult(true, entry.displayName,
                            entry.displayName + " has been launched successfully on Windows.");
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private LaunchResult launchDirect(String normalizedName, String originalName) {
        System.out.println("[AppLauncher] Attempting direct launch for: " + originalName);
        try {
            if (CURRENT_PLATFORM == Platform.WINDOWS) {
                // Try PowerShell Start-Process
                try {
                    ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "Start-Process '" + originalName + "'");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    boolean finished = process.waitFor(3, TimeUnit.SECONDS);
                    if (!finished || process.exitValue() == 0) {
                        return new LaunchResult(true, originalName,
                                "\"" + originalName + "\" has been launched on Windows.");
                    }
                } catch (Exception ignored) {}

                // Try cmd.exe /c start "" <name>
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "", originalName);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean exited = process.waitFor(2, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    return new LaunchResult(true, originalName,
                            "\"" + originalName + "\" has been launched on Windows.");
                }
            } else if (CURRENT_PLATFORM == Platform.MACOS) {
                ProcessBuilder pb = new ProcessBuilder("open", "-a", originalName);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    return new LaunchResult(true, originalName,
                            "\"" + originalName + "\" has been launched on macOS.");
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder(normalizedName);
                Process process = pb.start();
                boolean exited = process.waitFor(2, TimeUnit.SECONDS);
                if (!exited || process.exitValue() == 0) {
                    return new LaunchResult(true, originalName,
                            "\"" + originalName + "\" has been launched on Linux.");
                }
            }

            return new LaunchResult(false, originalName,
                    "Could not find or launch \"" + originalName + "\" on " + getPlatformDisplayName()
                            + ". Make sure it is installed and try the exact name.");
        } catch (Exception e) {
            System.err.println("[AppLauncher] Direct launch failed for \"" + originalName + "\": " + e.getMessage());
            return new LaunchResult(false, originalName,
                    "Could not find or launch \"" + originalName + "\" on " + getPlatformDisplayName()
                            + ". " + e.getMessage());
        }
    }

    private WebsiteEntry findBestWebsiteMatch(String normalizedQuery) {
        WebsiteEntry bestMatch = null;
        int bestScore = 0;

        for (WebsiteEntry entry : WEBSITE_CATALOG) {
            for (String alias : entry.aliases) {
                if (alias.equals(normalizedQuery)) {
                    return entry; // Exact match
                }
                if (normalizedQuery.contains(alias) || alias.contains(normalizedQuery)) {
                    int score = Math.min(alias.length(), normalizedQuery.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = entry;
                    }
                }
            }

            // Also check display name
            if (entry.displayName.toLowerCase().replace("/", "").replace(" ", "").contains(normalizedQuery.replace(" ", ""))) {
                return entry;
            }
        }

        return bestMatch;
    }

    private LaunchResult openWebsite(WebsiteEntry entry) {
        return openInBrowser(entry.url, entry.displayName);
    }

    private LaunchResult openUrl(String url, String displayName) {
        String fullUrl = url;
        if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
            fullUrl = "https://" + fullUrl;
        }
        return openInBrowser(fullUrl, displayName);
    }

    private static boolean looksLikeUrl(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().trim();
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("www.")
                || lower.matches("^[a-z0-9-]+\\.(com|org|net|io|co|dev|edu|gov|app|me|tv|ai)(/.*)?$");
    }

    private LaunchResult openInBrowser(String url, String displayName) {
        System.out.println("[AppLauncher] Opening website: " + displayName + " -> " + url);
        try {
            if (CURRENT_PLATFORM == Platform.WINDOWS) {
                // Method 1: PowerShell Start-Process (activates default browser in foreground)
                try {
                    ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "Start-Process '" + url + "'");
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    boolean done = p.waitFor(3, TimeUnit.SECONDS);
                    if (!done || p.exitValue() == 0) {
                        System.out.println("[AppLauncher] ✓ Opened " + displayName + " via PowerShell Start-Process");
                        return new LaunchResult(true, displayName,
                                displayName + " has been opened in your default browser (" + url + ").");
                    }
                } catch (Exception e1) {
                    System.err.println("[AppLauncher] PowerShell Start-Process failed: " + e1.getMessage());
                }

                // Method 2: cmd.exe /c start "" "<url>"
                try {
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "", url);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor(2, TimeUnit.SECONDS);
                    System.out.println("[AppLauncher] ✓ Opened " + displayName + " via cmd.exe start");
                    return new LaunchResult(true, displayName,
                            displayName + " has been opened in your default browser (" + url + ").");
                } catch (Exception e2) {
                    System.err.println("[AppLauncher] cmd.exe start failed: " + e2.getMessage());
                }

                // Method 3: Desktop.browse()
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    System.out.println("[AppLauncher] ✓ Opened " + displayName + " via Desktop.browse()");
                    return new LaunchResult(true, displayName,
                            displayName + " has been opened in your default browser (" + url + ").");
                }

                // Method 4: rundll32 FileProtocolHandler
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                return new LaunchResult(true, displayName,
                        displayName + " has been opened in your default browser (" + url + ").");

            } else if (CURRENT_PLATFORM == Platform.MACOS) {
                // macOS: open <url>
                ProcessBuilder pb = new ProcessBuilder("open", url);
                pb.redirectErrorStream(true);
                pb.start();
                System.out.println("[AppLauncher] ✓ Opened " + displayName + " on macOS via open");
                return new LaunchResult(true, displayName,
                        displayName + " has been opened in your default browser (" + url + ").");
            } else {
                // Linux: xdg-open <url>
                ProcessBuilder pb = new ProcessBuilder("xdg-open", url);
                pb.redirectErrorStream(true);
                pb.start();
                return new LaunchResult(true, displayName,
                        displayName + " has been opened in your default browser (" + url + ").");
            }
        } catch (Exception e) {
            System.err.println("[AppLauncher] Failed to open URL " + url + ": " + e.getMessage());
            return new LaunchResult(false, displayName,
                    "Could not open " + displayName + " (" + url + "): " + e.getMessage());
        }
    }
}
