package eliteslayer.util;

import org.dreambot.api.utilities.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Async Discord webhook dispatcher with a 2-second rate limit.
 * JSON special characters are properly escaped (optimisation 15).
 * Resources are properly closed (optimisation 16).
 */
public final class DiscordWebhook {

    private static final long RATE_LIMIT_MS = 2_000L;

    private final String webhookUrl;
    private final ScheduledExecutorService executor;
    private final AtomicLong lastSentTime = new AtomicLong(0L);

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.executor   = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EliteSlayer-Discord");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Sends {@code message} asynchronously, honouring the rate limit.
     */
    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        executor.submit(() -> {
            long now  = System.currentTimeMillis();
            long diff = now - lastSentTime.get();
            if (diff < RATE_LIMIT_MS) {
                try { Thread.sleep(RATE_LIMIT_MS - diff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
            lastSentTime.set(System.currentTimeMillis());
            dispatch(message);
        });
    }

    /** Must be called in onExit to shut down the background thread. */
    public void shutdown() {
        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    private void dispatch(String message) {
        HttpURLConnection conn = null;
        try {
            String payload = "{\"content\":\"" + escapeJson(message) + "\"}";
            URL url = new URL(webhookUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);

            byte[] body = payload.getBytes("UTF-8");
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(body);
                dos.flush();
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                // Drain the error stream so the connection can be reused
                drainStream(conn.getErrorStream());
                Logger.warn("[DiscordWebhook] HTTP " + code + " sending message");
            }
        } catch (Exception e) {
            Logger.error("[DiscordWebhook] Send failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void drainStream(InputStream stream) {
        if (stream == null) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            while (br.readLine() != null) { /* discard */ }
        } catch (IOException ignored) { /* best-effort */ }
    }

    /**
     * Escapes {@code \}, {@code "}, {@code \n}, {@code \r}, and {@code \t}
     * so the payload is valid JSON.
     */
    static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);      break;
            }
        }
        return sb.toString();
    }
}
