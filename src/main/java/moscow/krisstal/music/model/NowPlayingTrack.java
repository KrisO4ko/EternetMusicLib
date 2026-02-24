package moscow.krisstal.music.model;

import java.util.Objects;

public record NowPlayingTrack(
        String title,
        String artist,
        String album,
        PlaybackStatus status,
        String sourceApplication,
        String thumbnailPath
) {
    public NowPlayingTrack {
        title = Objects.requireNonNullElse(title, "Unknown");
        artist = Objects.requireNonNullElse(artist, "Unknown");
        album = Objects.requireNonNullElse(album, "Unknown");
        status = Objects.requireNonNullElse(status, PlaybackStatus.UNKNOWN);
        sourceApplication = Objects.requireNonNullElse(sourceApplication, "Unknown");
    }

    public NowPlayingTrack(String title, String artist, String album, PlaybackStatus status, String source) {
        this(title, artist, album, status, source, null);
    }

    public static NowPlayingTrack empty() {
        return new NowPlayingTrack(null, null, null, PlaybackStatus.STOPPED, null, null);
    }

    public boolean isPlaying() { return status == PlaybackStatus.PLAYING; }
    public boolean hasThumbnail() { return thumbnailPath != null && !thumbnailPath.isEmpty(); }
}