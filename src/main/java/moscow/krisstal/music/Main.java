package moscow.krisstal.music;

import moscow.krisstal.music.cover.CoverArtFetcher;
import moscow.krisstal.music.model.NowPlayingTrack;

public class Main {
    public static void main(String[] args) throws Exception {
        EternetMusicTracker tracker = new EternetMusicTracker();
        CoverArtFetcher cover = new CoverArtFetcher();
        String lastId = "";


        while (true) {
            NowPlayingTrack t = tracker.getCurrentTrackInterpolated();

            if (t.hasTime()) {
                String id = t.artist() + "|" + t.title();

                if (!id.equals(lastId)) {
                    System.out.println();
                    System.out.println(t.status() + " | " + t.artist() + " - " + t.title() + " [" + t.album() + "]");
                    cover.fetchUrl(t.artist(), t.title()).ifPresent(url -> System.out.println("Cover: " + url));
                    lastId = id;
                }

                int progress = (int)(t.getProgress() * 100);
                System.out.print("\rTime: " + t.getTimeFormatted() + " [" + progress + "%]    ");
            }

            Thread.sleep(200);
        }
    }
}