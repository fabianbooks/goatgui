# GoatGUI (Fabric 1.21.4)

Mod kliencki z ciemnym GUI: nagłówek z nickiem, lista graczy (2 kolumny, scroll),
przyciski "Search" oraz zakładki Gracz / Visuals / Ustawienia.

## Budowanie
Wymagania: JDK 21, Gradle (lub wrapper: `gradle wrapper` w katalogu projektu).

    gradle build

Gotowy plik: `build/libs/goatgui-1.0.0.jar` -> wrzuć do `.minecraft/mods`
razem z Fabric Loader 1.21.4 i Fabric API.

## Użycie
Right Shift otwiera menu (klawisz zmienisz w Sterowaniu -> GoatGUI).
Przycisk "Search" wypisuje na czacie nick i ping wybranego gracza.
