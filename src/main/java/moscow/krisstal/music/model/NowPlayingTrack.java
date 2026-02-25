package moscow.krisstal.music.model;

import java.util.Objects;

public record NowPlayingTrack(
        String title,
        String artist,
        String album,
        PlaybackStatus status,
        String sourceApplication,
        String thumbnailPath,
        long positionMs,
        long durationMs
) {
    public NowPlayingTrack {
        title = Objects.requireNonNullElse(title, "Unknown");
        artist = Objects.requireNonNullElse(artist, "Unknown");
        album = Objects.requireNonNullElse(album, "Unknown");
        status = Objects.requireNonNullElse(status, PlaybackStatus.UNKNOWN);
        sourceApplication = Objects.requireNonNullElse(sourceApplication, "Unknown");
    }

    public NowPlayingTrack(String title, String artist, String album, PlaybackStatus status, String source) {
        this(title, artist, album, status, source, null, 0, 0);
    }

    public static NowPlayingTrack empty() {
        return new NowPlayingTrack(null, null, null, PlaybackStatus.STOPPED, null, null, 0, 0);
    }

    public boolean isPlaying() { return status == PlaybackStatus.PLAYING; }
    public boolean hasThumbnail() { return thumbnailPath != null && !thumbnailPath.isEmpty(); }
    public boolean hasTime() { return durationMs > 0; }

    public String getPositionFormatted() { return formatTime(positionMs); }
    public String getDurationFormatted() { return formatTime(durationMs); }
    public String getTimeFormatted() { return getPositionFormatted() + " / " + getDurationFormatted(); }
    public double getProgress() { return durationMs > 0 ? (double) positionMs / durationMs : 0; }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%d:%02d", min, sec);
    }
}