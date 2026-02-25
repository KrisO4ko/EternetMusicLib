package moscow.krisstal.music;

import moscow.krisstal.music.model.NowPlayingTrack;
import moscow.krisstal.music.provider.*;
import java.util.Optional;
import java.util.concurrent.*;

public class EternetMusicTracker {

    private final LocalMediaProvider provider;
    private final ScheduledExecutorService executor;
    private volatile NowPlayingTrack lastTrack = null;
    private volatile long trackStartSystemTime = 0;
    private volatile long trackStartPosition = 0;

    public EternetMusicTracker() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) provider = new WindowsMediaProvider();
        else if (os.contains("mac")) provider = new MacOsMediaProvider();
        else provider = new LinuxMediaProvider();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MusicTracker");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::updateTrack, 0, 3, TimeUnit.SECONDS);
    }

    private void updateTrack() {
        try {
            provider.getCurrentTrack().ifPresent(t -> {
                boolean isNewTrack = lastTrack == null ||
                        !t.title().equals(lastTrack.title()) ||
                        !t.artist().equals(lastTrack.artist());

                boolean seeked = lastTrack != null &&
                        Math.abs(t.positionMs() - getInterpolatedPosition()) > 3000;

                if (isNewTrack || seeked || !t.isPlaying()) {
                    trackStartPosition = t.positionMs();
                    trackStartSystemTime = System.currentTimeMillis();
                }

                lastTrack = t;
            });
        } catch (Exception ignored) {}
    }

    private long getInterpolatedPosition() {
        if (lastTrack == null || trackStartSystemTime == 0) return 0;
        if (!lastTrack.isPlaying()) return trackStartPosition;

        long elapsed = System.currentTimeMillis() - trackStartSystemTime;
        long position = trackStartPosition + elapsed;
        return Math.min(position, lastTrack.durationMs());
    }

    public Optional<NowPlayingTrack> getCurrentTrack() {
        return Optional.ofNullable(lastTrack);
    }

    public NowPlayingTrack getCurrentTrackInterpolated() {
        if (lastTrack == null) return NowPlayingTrack.empty();

        return new NowPlayingTrack(
                lastTrack.title(),
                lastTrack.artist(),
                lastTrack.album(),
                lastTrack.status(),
                lastTrack.sourceApplication(),
                lastTrack.thumbnailPath(),
                getInterpolatedPosition(),
                lastTrack.durationMs()
        );
    }

    public String getProviderName() { return provider.getProviderName(); }
    public void shutdown() { executor.shutdown(); }
}