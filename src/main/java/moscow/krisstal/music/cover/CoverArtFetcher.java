package moscow.krisstal.music.cover;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.*;

public class CoverArtFetcher {

    private String lastQuery = "", lastUrl = "";

    public Optional<String> fetchUrl(String artist, String title) {
        String q = artist + " " + title;
        if (q.equals(lastQuery) && !lastUrl.isEmpty()) return Optional.of(lastUrl);
        lastQuery = q;
        lastUrl = "";
        try {
            String enc = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String json = http("https://api.deezer.com/search?q=" + enc + "&limit=1");
            if (json != null && !json.contains("\"data\":[]")) {
                Matcher m = Pattern.compile("\"cover_big\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (m.find()) { lastUrl = m.group(1).replace("\\/", "/"); return Optional.of(lastUrl); }
            }
            json = http("https://itunes.apple.com/search?term=" + enc + "&media=music&limit=1");
            if (json != null) {
                Matcher m = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (m.find()) { lastUrl = m.group(1).replace("100x100", "600x600"); return Optional.of(lastUrl); }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private String http(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestProperty("User-Agent", "EternetMusicLib/1.0");
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            if (c.getResponseCode() != 200) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder(); String l; while ((l = r.readLine()) != null) sb.append(l); return sb.toString();
            }
        } catch (Exception e) { return null; }
    }
}