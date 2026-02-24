package moscow.krisstal.music;

import moscow.krisstal.music.model.NowPlayingTrack;
import moscow.krisstal.music.provider.*;
import java.util.Optional;

public class EternetMusicTracker {

    private final LocalMediaProvider provider;

    public EternetMusicTracker() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) provider = new WindowsMediaProvider();
        else if (os.contains("mac")) provider = new MacOsMediaProvider();
        else provider = new LinuxMediaProvider();
    }

    public Optional<NowPlayingTrack> getCurrentTrack() { return provider.isSupported() ? provider.getCurrentTrack() : Optional.empty(); }
    public String getProviderName() { return provider.getProviderName(); }
}