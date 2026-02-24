Java-библиотека для получения информации о музыке, которая играет прямо сейчас на компьютере.

Разработана для чит-клиента Minecraft — **Eternet**. Используется для отображения текущего трека в игровом HUD.

## Возможности
- Получение названия трека, артиста, альбома
- Определение статуса воспроизведения (играет/пауза/стоп)
- Определение источника (Spotify, Chrome, Firefox и др.)
- Получение ссылки на обложку альбома
- Кроссплатформенность (Windows, macOS, Linux)

## Проверено на Windows 10 (Spotify)

## Как это работает

### Получение информации о треке

Библиотека читает данные напрямую из системного API:
- **Windows**: System Media Transport Controls (SMTC) через PowerShell
- **macOS**: AppleScript
- **Linux**: MPRIS через D-Bus (playerctl)

Никаких API-ключей не требуется. Работает с любым плеером, который интегрирован с системными медиа-контролами.

### Получение обложки

Обложки загружаются через публичные API музыкальных сервисов:

1. **Deezer API** (приоритет) — поиск по артисту и названию трека, получение `cover_big` (500x500)
2. **iTunes API** (резерв) — если Deezer не нашёл, поиск через iTunes, получение `artworkUrl` (600x600)

API-ключи не требуются. Библиотека возвращает прямую ссылку на изображение.

## Установка

Скопируй пакет `com.eternet.music` в свой проект.

Требуется Java 17+.

## Пример использования

### Базовый пример

```java
import moscow.krisstal.music.EternetMusicTracker;
import moscow.krisstal..music.cover.CoverArtFetcher;
import moscow.krisstal.music.model.NowPlayingTrack;
import java.util.Optional;

public class Example {
    public static void main(String[] args) {
        EternetMusicTracker tracker = new EternetMusicTracker();
        CoverArtFetcher cover = new CoverArtFetcher();
        
        Optional<NowPlayingTrack> track = tracker.getCurrentTrack();
        
        if (track.isPresent()) {
            NowPlayingTrack t = track.get();
            System.out.println("Сейчас играет: " + t.artist() + " - " + t.title());
            System.out.println("Альбом: " + t.album());
            System.out.println("Статус: " + t.status());
            System.out.println("Источник: " + t.sourceApplication());
            cover.fetchUrl(t.artist(), t.title()).ifPresent(url -> System.out.println("Обложка: " + url));
        } else {
            System.out.println("Ничего не играет");
        }
    }
}
