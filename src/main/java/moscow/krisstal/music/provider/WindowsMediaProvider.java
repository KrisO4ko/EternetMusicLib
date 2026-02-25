package moscow.krisstal.music.provider;

import moscow.krisstal.music.model.NowPlayingTrack;
import moscow.krisstal.music.model.PlaybackStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WindowsMediaProvider implements LocalMediaProvider {

    private static final String PS_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n" +
                    "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n" +
                    "$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n" +
                    "Function Await($t, $r) { $m = $asTask.MakeGenericMethod($r); $n = $m.Invoke($null, @($t)); $n.Wait(-1) | Out-Null; $n.Result }\n" +
                    "try { [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null } catch { exit 1 }\n" +
                    "try {\n" +
                    "  $sm = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n" +
                    "  $cs = $sm.GetCurrentSession()\n" +
                    "  if ($null -eq $cs) { Write-Output 'NO_SESSION'; exit 0 }\n" +
                    "  $mp = Await ($cs.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n" +
                    "  $pi = $cs.GetPlaybackInfo()\n" +
                    "  $tp = $cs.GetTimelineProperties()\n" +
                    "  $st = 'STOPPED'\n" +
                    "  if ($null -ne $pi) { $sv = [string]$pi.PlaybackStatus; if ($sv -like '*Playing*') { $st = 'PLAYING' } elseif ($sv -like '*Paused*') { $st = 'PAUSED' } }\n" +
                    "  $pos = 0; $dur = 0\n" +
                    "  if ($null -ne $tp) { $pos = [long]$tp.Position.TotalMilliseconds; $dur = [long]$tp.EndTime.TotalMilliseconds }\n" +
                    "  Write-Output '===START==='\n" +
                    "  Write-Output \"TITLE=$([string]$mp.Title)\"\n" +
                    "  Write-Output \"ARTIST=$([string]$mp.Artist)\"\n" +
                    "  Write-Output \"ALBUM=$([string]$mp.AlbumTitle)\"\n" +
                    "  Write-Output \"STATUS=$st\"\n" +
                    "  Write-Output \"SOURCE=$([string]$cs.SourceAppUserModelId)\"\n" +
                    "  Write-Output \"POSITION=$pos\"\n" +
                    "  Write-Output \"DURATION=$dur\"\n" +
                    "  Write-Output '===END==='\n" +
                    "} catch { Write-Output \"ERROR=$($_.Exception.Message)\" }\n";

    @Override
    public Optional<NowPlayingTrack> getCurrentTrack() {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", PS_SCRIPT);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = r.readLine()) != null) out.append(line).append("\n");
            }
            if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); return Optional.empty(); }
            return parse(out.toString());
        } catch (Exception e) { return Optional.empty(); }
    }

    private Optional<NowPlayingTrack> parse(String out) {
        if (out.contains("NO_SESSION") || !out.contains("===START===")) return Optional.empty();
        Map<String, String> d = new HashMap<>();
        boolean in = false;
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.equals("===START===")) { in = true; continue; }
            if (line.equals("===END===")) break;
            if (in && line.contains("=")) { int i = line.indexOf('='); d.put(line.substring(0, i), line.substring(i + 1)); }
        }
        String t = d.getOrDefault("TITLE", ""), a = d.getOrDefault("ARTIST", "");
        if (t.isEmpty() && a.isEmpty()) return Optional.empty();
        PlaybackStatus st = switch (d.getOrDefault("STATUS", "")) { case "PLAYING" -> PlaybackStatus.PLAYING; case "PAUSED" -> PlaybackStatus.PAUSED; default -> PlaybackStatus.STOPPED; };
        String src = d.getOrDefault("SOURCE", "").toLowerCase();
        String source = src.contains("spotify") ? "Spotify" : src.contains("chrome") ? "Chrome" : src.contains("firefox") ? "Firefox" : src.contains("yandex") ? "Yandex" : d.getOrDefault("SOURCE", "");
        long pos = parseLong(d.get("POSITION"));
        long dur = parseLong(d.get("DURATION"));
        return Optional.of(new NowPlayingTrack(t.isEmpty() ? null : t, a.isEmpty() ? null : a, d.get("ALBUM"), st, source, null, pos, dur));
    }

    private long parseLong(String s) {
        try { return s != null ? Long.parseLong(s.trim()) : 0; } catch (Exception e) { return 0; }
    }

    @Override public boolean isSupported() { return System.getProperty("os.name", "").toLowerCase().contains("windows"); }
    @Override public String getProviderName() { return "Windows SMTC"; }
}