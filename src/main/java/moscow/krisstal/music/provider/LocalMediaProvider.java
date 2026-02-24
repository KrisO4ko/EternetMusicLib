package moscow.krisstal.music.provider;

import moscow.krisstal.music.model.NowPlayingTrack;
import java.util.Optional;

public interface LocalMediaProvider {
    
    Optional<NowPlayingTrack> getCurrentTrack();
    
    boolean isSupported();
    
    String getProviderName();
}