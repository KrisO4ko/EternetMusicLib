package moscow.krisstal.music.provider;

import moscow.krisstal.music.model.NowPlayingTrack;
import moscow.krisstal.music.model.PlaybackStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LinuxMediaProvider implements LocalMediaProvider {

    private static final String SCRIPT = "if command -v playerctl &>/dev/null; then P=$(playerctl -l 2>/dev/null|head -1); if [ -n \"$P\" ]; then echo \"TITLE=$(playerctl -p $P metadata title 2>/dev/null)\"; echo \"ARTIST=$(playerctl -p $P metadata artist 2>/dev/null)\"; echo \"ALBUM=$(playerctl -p $P metadata album 2>/dev/null)\"; echo \"STATUS=$(playerctl -p $P status 2>/dev/null)\"; echo \"SOURCE=$P\"; echo \"POSITION=$(playerctl -p $P position 2>/dev/null | awk '{print int($1*1000000)}')\"; echo \"DURATION=$(playerctl -p $P metadata mpris:length 2>/dev/null)\"; else echo NO_SESSION; fi; else echo NO_SESSION; fi";

    @Override
    public Optional<NowPlayingTrack> getCurrentTrack() {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", SCRIPT);
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
        PlaybackStatus st = "Playing".equalsIgnoreCase(d.get("STATUS")) ? PlaybackStatus.PLAYING : PlaybackStatus.PAUSED;
        long pos = parseLong(d.get("POSITION")) / 1000;
        long dur = parseLong(d.get("DURATION")) / 1000;
        return Optional.of(new NowPlayingTrack(t, a, d.get("ALBUM"), st, d.getOrDefault("SOURCE", "Unknown"), null, pos, dur));
    }

    private long parseLong(String s) {
        try { return s != null ? Long.parseLong(s.trim()) : 0; } catch (Exception e) { return 0; }
    }

    @Override public boolean isSupported() { String os = System.getProperty("os.name", "").toLowerCase(); return os.contains("nix") || os.contains("nux"); }
    @Override public String getProviderName() { return "Linux MPRIS"; }
}