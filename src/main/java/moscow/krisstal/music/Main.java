package moscow.krisstal.music;

import moscow.krisstal.music.cover.CoverArtFetcher;
import moscow.krisstal.music.model.NowPlayingTrack;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        EternetMusicTracker tracker = new EternetMusicTracker();
        CoverArtFetcher cover = new CoverArtFetcher();
        String lastId = "";

        while (true) {
            Optional<NowPlayingTrack> track = tracker.getCurrentTrack();
            if (track.isPresent()) {
                NowPlayingTrack t = track.get();
                String id = t.artist() + "|" + t.title();
                if (!id.equals(lastId)) {
                    System.out.println(t.status() + " | " + t.artist() + " - " + t.title() + " [" + t.album() + "] via " + t.sourceApplication());
                    cover.fetchUrl(t.artist(), t.title()).ifPresent(url -> System.out.println("Cover: " + url));
                    lastId = id;
                }
            }
            Thread.sleep(2000);
        }
    }
}