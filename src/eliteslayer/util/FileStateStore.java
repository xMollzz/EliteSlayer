package eliteslayer.util;

import org.dreambot.api.utilities.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight file-based key/value state store that supports crash-resume.
 * Every 30 seconds it checks whether the config file has been modified and
 * reloads it automatically (hot-reload, optimisation 12).
 */
public final class FileStateStore {

    private static final long RELOAD_INTERVAL_MS = 30_000L;

    private final File file;
    private final Map<String, String> data = new HashMap<>();

    private long lastModified  = 0L;
    private long lastCheckTime = 0L;

    public FileStateStore(File file) {
        this.file = file;
        load();
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    public void set(String key, String value) {
        data.put(key, value);
    }

    public String get(String key, String defaultValue) {
        maybeReload();
        return data.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public long getLong(String key, long defaultValue) {
        try { return Long.parseLong(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key, null);
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v);
    }

    /** Flushes all in-memory state to disk. */
    public void save() {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    pw.println(escape(entry.getKey()) + "=" + escape(entry.getValue()));
                }
            }
            lastModified = file.lastModified();
        } catch (IOException e) {
            Logger.error("[FileStateStore] Save failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                    //
    // ------------------------------------------------------------------ //

    private void load() {
        if (!file.exists()) return;
        data.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key   = unescape(line.substring(0, eq));
                String value = unescape(line.substring(eq + 1));
                data.put(key, value);
            }
            lastModified = file.lastModified();
        } catch (IOException e) {
            Logger.error("[FileStateStore] Load failed: " + e.getMessage());
        }
    }

    /** Checks for file changes every RELOAD_INTERVAL_MS and reloads if needed. */
    private void maybeReload() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < RELOAD_INTERVAL_MS) return;
        lastCheckTime = now;
        if (!file.exists()) return;
        long mod = file.lastModified();
        if (mod != lastModified) {
            load();
            Logger.log("[FileStateStore] Config reloaded from " + file.getName());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("=", "\\=").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                if      (c == '\\') sb.append('\\');
                else if (c == '=')  sb.append('=');
                else if (c == 'n')  sb.append('\n');
                else { sb.append('\\'); sb.append(c); }
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
