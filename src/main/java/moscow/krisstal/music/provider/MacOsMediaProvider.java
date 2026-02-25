package moscow.krisstal.music.provider;

import moscow.krisstal.music.model.NowPlayingTrack;
import moscow.krisstal.music.model.PlaybackStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MacOsMediaProvider implements LocalMediaProvider {

    private static final String SCRIPT = "tell application \"Spotify\"\nif player state is playing then\nset pos to player position\nset dur to duration of current track\nreturn \"TITLE=\" & name of current track & \"\nARTIST=\" & artist of current track & \"\nALBUM=\" & album of current track & \"\nSTATUS=PLAYING\nPOSITION=\" & (round (pos * 1000)) & \"\nDURATION=\" & dur\nelse if player state is paused then\nset pos to player position\nset dur to duration of current track\nreturn \"TITLE=\" & name of current track & \"\nARTIST=\" & artist of current track & \"\nALBUM=\" & album of current track & \"\nSTATUS=PAUSED\nPOSITION=\" & (round (pos * 1000)) & \"\nDURATION=\" & dur\nelse\nreturn \"NO_SESSION\"\nend if\nend tell";

    @Override
    public Optional<NowPlayingTrack> getCurrentTrack() {
        try {
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", SCRIPT);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = r.readLine()) != null) out.append(line).append("\n");
            }
            if (!p.waitFor(5, TimeUnit.SECONDS)) { p.destroyForcibly(); return Optional.empty(); }
            return parse(out.toString());
        } catch (Exception e) { return Optional.empty(); }
    }

    private Optional<NowPlayingTrack> parse(String out) {
        if (out.contains("NO_SESSION")) return Optional.empty();
        Map<String, String> d = new HashMap<>();
        for (String line : out.split("\n")) { if (line.contains("=")) { int i = line.indexOf('='); d.put(line.substring(0, i), line.substring(i + 1)); } }
        String t = d.getOrDefault("TITLE", ""), a = d.getOrDefault("ARTIST", "");
        if (t.isEmpty() && a.isEmpty()) return Optional.empty();
        PlaybackStatus st = "PLAYING".equals(d.get("STATUS")) ? PlaybackStatus.PLAYING : PlaybackStatus.PAUSED;
        long pos = parseLong(d.get("POSITION"));
        long dur = parseLong(d.get("DURATION"));
        return Optional.of(new NowPlayingTrack(t, a, d.get("ALBUM"), st, "Spotify", null, pos, dur));
    }

    private long parseLong(String s) {
        try { return s != null ? Long.parseLong(s.trim()) : 0; } catch (Exception e) { return 0; }
    }

    @Override public boolean isSupported() { return System.getProperty("os.name", "").toLowerCase().contains("mac"); }
    @Override public String getProviderName() { return "macOS AppleScript"; }
}