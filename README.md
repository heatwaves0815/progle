# Progle – KI-gestützter Java-Lernassistent

**Progle** ist ein Desktop-Programm (Java 21 + JavaFX), in dem du Java von Grund auf lernst.
Eine KI (über **OpenRouter**) agiert dabei als persönlicher Tutor **im Programm selbst**:

| Funktion | Beschreibung |
|---|---|
| Chat mit KI-Tutor | Frage jederzeit im Chat – geduldige Antworten in einfacher Sprache |
| Lernplan | Beim ersten Start erstellt die KI aus deinem Namen + Vorkenntnissen einen Plan: Variablen → Schleifen → Methoden → OOP |
| Übungsaufgaben | Zu jeder Lektion gibt es eine Aufgabe, gelöst wird sie im eingebauten Code-Editor mit Java-Syntax-Highlighting |
| Code-Prüfung | Dein Code wird lokal kompiliert **und** ausgeführt; die KI prüft ihn danach und gibt strukturiertes Feedback (Gut gemacht / Was fehlt / Fehler) |
| Kein Spickzettel | Die KI zeigt **niemals die komplette Lösung** – nur Hinweise in max. 3 Stufen: Denkanstoß → Tipp → Teilansatz |
| Fehler-Erklärung | Compiler-Fehler und Exceptions werden dir auf Deutsch erklärt statt einfach weggefixt |
| Beispiele | Auf Knopfdruck zeigt die KI ein vollständiges, lauffähiges Beispiel – nie die Lösung der aktuellen Aufgabe |
| Fortschritt speichern | Name, Vorkenntnisse, Lektionen, Code-Entwürfe und Hinweis-Konto werden lokal als JSON gespeichert |

---

## 1. Voraussetzungen

**Gute Nachricht:** Für dieses Projekt sind bereits fertig installiert:

- **JDK 21** (Temurin) in `F:\my_programs\jdk-21`
- **Apache Maven 3.9.9** in `F:\my_programs\maven`

Die Datei **`start.bat`** verwendet automatisch beides – und zwar **nur für Progle**:
das systemweite Java (z. B. dein Java 17) bleibt komplett unberührt, weil `JAVA_HOME`
nur innerhalb des Skripts umgestellt wird.

Wenn du lieber manuell in der Konsole arbeitest, brauchst du:

1. **JDK 21 oder neuer** – hier schon vorhanden: `F:\my_programs\jdk-21`
2. **Apache Maven 3.9+** – hier schon vorhanden: `F:\my_programs\maven`
3. (Optional) Eine IDE wie IntelliJ IDEA, Eclipse oder VS Code.

## 2. API-Key eintragen (einmalig)

Die KI-Anbindung läuft über **OpenRouter** (https://openrouter.ai). Der Key liegt **nur in einer lokalen Datei**, niemals im Code.

1. Erstelle einen API-Key auf https://openrouter.ai/keys
2. Kopiere `config.example.json` und benenne die Kopie um in **`config.json`**
   (liegt direkt neben der `pom.xml`, also im Projektordner `F:\my_programs\progle\`)
3. Trage deinen Key ein:

```json
{
  "apiKey": "sk-or-...dein-key...",
  "model": "z-ai/glm-5.3-flash",
  "baseUrl": "https://openrouter.ai/api/v1"
}
```

**Alternativen:**
- Datei `%USERPROFILE%\.progle\config.json` (wird ebenfalls gelesen)
- Umgebungsvariable `OPENROUTER_API_KEY` (dann reicht die Datei ohne Key)

**Hinweise:**
- `model` ist der Modellname aus der config.json – er ist **nicht** im Code hartcodiert und jederzeit austauschbar gegen jede Modell-ID von https://openrouter.ai/models (z. B. `openai/gpt-4o-mini`, `anthropic/claude-...`).
- `baseUrl` erlaubt auch kompatible Anbieter, die dieselbe Chat-Completions-API anbieten.
- `config.json` steht in der `.gitignore` – nie committen oder weitergeben!
- Ohne Key startet Progle trotzdem: Du bekommst einen Standard-Lernplan, aber Chat, Prüfung und Hinweise funktionieren erst mit Key (das Programm weist dich im Chat darauf hin).

## 3. Bauen und Starten

**Variante 1 - Progle.exe (empfohlen, ohne Terminal):**
Doppelklick auf **`dist\Progle\Progle.exe`** (bzw. Desktop-Verknüpfung "Progle").
Die Exe bringt ein eigenes Java mit - kein JDK, kein Maven, kein Terminal nötig.
Beim Start erscheint eine Ladeanimation mit geschätzter Ladezeit (meist 3-10 Sekunden).
Erster Start: Windows-SmartScreen-Hinweis → "Weitere Informationen" → "Trotzdem ausführen".
Wichtig: Der Ordner `dist\Progle\` muss zusammen bleiben (darin liegen Lehrplan, Stimme und Java).

**Variante 2 - start.bat (Entwicklermodus):**
`start.bat` doppelklicken - sie stellt automatisch JDK 21 (`F:\my_programs\jdk-21`)
und Maven (`F:\my_programs\maven`) bereit, ohne dein systemweites Java zu verändern.
Die Exe wird bei Code-Änderungen neu gebaut mit: `mvn package` + `jpackage` (siehe unten).

```bat
mvn -DskipTests package
mvn dependency:copy-dependencies "-DoutputDirectory=target/app-input" "-DincludeScope=runtime"
copy target\progle-1.0.0.jar target\app-input\
jpackage --type app-image --name Progle --input target\app-input --main-jar progle-1.0.0.jar --main-class de.progle.Launcher --dest dist --icon target\app-input\proggle.ico "--java-options=-Dfile.encoding=UTF-8"
xcopy /E /I curricula dist\Progle\curricula
xcopy /E /I tts dist\Progle\tts
copy config.json dist\Progle\
```

**Ablauf beim Start:**
1. Ein Info-Fenster erscheint kurz und schließt sich selbst.
2. Der eigentliche Start läuft im Hintergrund in einem **minimierten Konsolenfenster**
   „Progle-Start (NICHT schliessen)" in der Taskleiste (dort sieht man Details bei Problemen).
3. Das **Progle-Fenster** öffnet sich nach ca. 10–60 Sekunden automatisch.
4. Progle beenden: das Progle-Fenster schließen (X).

Alternativ manuell im Projektordner `F:\my_programs\progle`:

```bat
:: JDK 21 nur für diese Konsolensitzung aktivieren (systemweite Einstellungen bleiben unverändert)
set "JAVA_HOME=F:\my_programs\jdk-21"

:: starten (kompiliert bei Bedarf automatisch mit)
F:\my_programs\maven\bin\mvn.cmd javafx:run
```

Weitere Befehle:

```bat
F:\my_programs\maven\bin\mvn.cmd clean package   :: bauen ohne Starten
```

**In der IDE:** Projekt als Maven-Projekt öffnen und die Klasse
`src/main/java/de/progle/Launcher.java` ausführen (dieser Startpunkt umgeht das bekannte
"JavaFX runtime components are missing"-Problem).

## 4. Erste Schritte

1. Beim ersten Start öffnet sich das **Willkommensfenster**:
   Name eingeben → Vorkenntnisse wählen (keine / etwas / erfahren) → **"Lernplan erstellen"**.
2. Die KI erstellt deinen persönlichen Lernplan (Variablen → Schleifen → Methoden → OOP).
   Ohne Internet/API-Key greift ein vorbereiteter Standard-Lernplan.
3. Links siehst du deinen **Lernplan mit Fortschrittsbalken** und Abhaken.
4. Im Tab **"Lektion"** steht die Erklärung und die Übungsaufgabe. Löse sie im Code-Editor darunter.
5. Buttons:
   - **Abgeben und prüfen** – dein Code wird lokal kompiliert, ausgeführt und von der KI bewertet.
     Bei "ERGEBNIS: BESTANDEN" wird die Lektion automatisch abgehakt.
   - **Hinweis (Stufe 1–3)** – Denkanstoß → Tipp → Teilansatz (nie die Lösung). Pro Lektion max. 3.
   - **Beispiel ansehen** – ein anderes, lauffähiges Beispiel zum Thema.
6. Im Tab **"Chat mit dem Tutor"** kannst du jederzeit Fragen stellen.
7. Fortschritt, Code-Entwürfe und genutzte Hinweise werden automatisch gespeichert –
   beim nächsten Start machst du genau da weiter.

## 5. Wo werden Daten gespeichert?

| Datei | Inhalt |
|---|---|
| `config.json` (Projektordner oder `%USERPROFILE%\.progle\`) | API-Key, Modell, Basis-URL |
| `curricula/<sprache>/` | Die Lehrpläne (Daten, kein Code) |
| `%USERPROFILE%\.progle\progress.json` | Name, Vorkenntnisse, Fortschritt, Code-Entwürfe, Quiz-Ergebnisse |

Lernplan zurücksetzen: Button **"Zur Sprachauswahl"** in der Seitenleiste
oder `%USERPROFILE%\.progle\progress.json` löschen.

## 6. Der Lehrplan (Curricula)

Der komplette Unterricht liegt als **Daten** in `curricula/java/` - die App lädt ihn dynamisch:

```
curricula/java/
├── curriculum.json          Struktur: Module, Reihenfolge, Voraussetzungen
├── lessons/                 eine JSON-Datei pro Lektion
│   ├── m1-l01-was-ist-programmieren.json
│   └── ...
└── projects/                die Abschlussprojekte
    ├── p1-kontoverwaltung.json
    ├── p2-vokabeltrainer.json
    └── p3-schere-stein-papier.json
```

Eine Lektionsdatei enthält: `titel`, `typ` (lektion / modul-quiz / projekt), `lernziele`,
`erklärung` (Markdown), `beispiel-code` (lauffähig), `übungen` (je: aufgabenstellung,
`start-code`, 3 `hinweise`, `lösung`, `prüfung` mit erwarteter Ausgabe + optionaler
simulierter Tastatur-`eingabe`), `quiz` (Multiple Choice) und `typische-fehler`.

**Übungen werden lokal validiert**: Dein Code wird kompiliert, mit simulierter Eingabe
gestartet und die Ausgabe mit `prüfung.erwartet` verglichen. Bestanden = Häkchen.
Die KI liefert zusätzlich persönliches Feedback - die `lösung` sieht sie zur Orientierung,
zeigt sie aber niemals an.

### Zweites Curriculum ergänzen (z. B. Python)

1. Neuen Ordner anlegen: `curricula/python/` mit eigenem `curriculum.json` (gleiche Struktur: `id: "python"`, `module` mit `lektionen`-IDs).
2. Lektionen nach **demselben Schema** in `curricula/python/lessons/` ablegen (Beispiel-Code und Lösungen in Python, `prüfung.erwartet` mit der echten Python-Ausgabe).
3. App neu starten - die Sprache erscheint **automatisch** im Sprachauswahl-Bildschirm.
4. Damit Python-Code auch ausgeführt wird, erweitert man `CodeRunnerService` um einen Sprach-Lauf (z. B. `python <datei>` statt Kompilieren) - die Ausgabevergleich-Logik bleibt identisch.

## 7. Ohne Umlaut-Tastatur schreiben (ae/oe/ue/ss)

Keine ä/ö/ü/ß auf der Tastatur? Einfach `
ae`, `oe`, `ue` und `ss` tippen:

- **Im Chat und beim Namen:** `koennen` → `können`, `Gruesse` → `Grüße`, `muede` → `müde`. Die Ersetzung ist kontextsicher: `neue`, `aktuell`, `Quelle`, `bequem` und `Wasser` bleiben unverändert.
- **In Übungen:** Wenn du in Strings `Gruesse` statt `Grüße` schreibst, setzt Progle die Anführungszeichen-Inhalte bei der Abgabe automatisch um - dein Code selbst (Variablen, Schlüsselwörter) bleibt unangetastet.
- **Ausgabevergleich:** Die Prüfung akzeptiert ae/oe/ue/ss und ä/ö/ü/ß als gleichwertig - beides zählt als richtig.

## 8. Designs (Themes)

In der Seitenleiste unter **Design** waehlst du live zwischen vier Themes:
- **Dunkel Violett** (Standard)
- **Mitternacht** (dunkles Marineblau, Cyan-Akzent)
- **Nordisch** (Nord-Palette)
- **Hell**

Die Auswahl wird in `%USERPROFILE%\.progle\settings.json` gespeichert und gilt ab dem naechsten Start automatisch. Eigene Themes: `resources/themes/theme-<name>.css` anlegen (muss nur die Design-Tokens im `.root`-Block setzen) und die Liste in `MainView.java` ergaenzen.

## 8. Sprachausgabe (TTS - lokal, mitgeliefert)

Progle liest dir Lektionen und Aufgaben laut vor - komplett offline:
- Engine: **Piper TTS** (neural, schnell) liegt fertig in `tts/piper/`
- Stimme: **de_DE Thorsten (high)** - eine der besten deutschen lokalen Stimmen, in `tts/voices/`
- Nutzung: Button **'Vorlesen'** neben dem Lektionstitel, **'Aufgabe vorlesen'** in der Übungszeile; während des Vorlesens wird der Button zu **'Stopp'**
- Weitere Stimmen: von https://huggingface.co/rhasspy/piper-voices (Ordner de/de_DE/...) die `.onnx` + `.onnx.json` in `tts/voices/` legen - die App nutzt automatisch die erste gefundene Stimme
- Falls der Ordner `tts/` fehlt, bleibt die App voll nutzbar - die Vorlesen-Buttons melden nur, dass die Sprachausgabe nicht installiert ist

## 9. Fehlerbehebung

| Problem | Lösung |
|---|---|
| `mvn` wird nicht gefunden | Maven installieren und in `PATH` aufnehmen (siehe https://maven.apache.org/install.html) |
| "JavaFX runtime components are missing" | Starte über `mvn javafx:run` bzw. `start.bat` oder in der IDE über `de.progle.Launcher` |
| "release version 21 not supported" beim Bauen | Ein JDK 21+ als `JAVA_HOME` setzen – hier schon vorhanden: `set JAVA_HOME=F:\my_programs\jdk-21` |
| Chat/Prüfung sagt "Kein API-Key konfiguriert" | `config.json` anlegen und Key eintragen (Abschnitt 2) |
| HTTP 401 von der API | OpenRouter-API-Key ist falsch oder abgelaufen – neuen Key auf https://openrouter.ai/keys erstellen |
| HTTP 402 von der API | Guthaben auf OpenRouter aufgebraucht – aufladen auf https://openrouter.ai/credits |
| HTTP 404 von der API | Modellname in `config.json` prüfen (gültige IDs: https://openrouter.ai/models) |
| HTTP 429 von der API | Rate-Limit erreicht – kurz warten |
| Lokale Kompilierung schlägt fehl ("Kein JDK-Compiler verfügbar") | JRE statt JDK installiert – JDK 21+ installieren |
| Lernplan lässt sich nicht von der KI erstellen | Netzwerk/Key prüfen; Progle nutzt automatisch den Standard-Lernplan |

## 10. Projektstruktur

```
progle/
├── pom.xml                          Maven-Konfiguration (Java 21, JavaFX, RichTextFX, Gson)
├── config.example.json              Vorlage für config.json (API-Key)
├── curricula/java/                  Der komplette Lehrplan als JSON-Daten
├── start.bat                        Startskript
├── README.md
└── src/main/
    ├── java/de/progle/
    │   ├── Launcher.java            Einstiegspunkt (IDE-freundlich)
    │   ├── model/                   Datenmodelle
    │   │   ├── KnowledgeLevel.java
    │   │   ├── UserProfile.java
    │   │   ├── Lesson.java
    │   │   ├── ChatMessage.java
    │   │   └── ProgressData.java
    │   ├── service/                 Geschäftslogik (KI, Speicherung, Compiler)
    │   │   ├── AiConfig.java
    │   │   ├── ConfigService.java       liest config.json / Umgebungsvariable
    │   │   ├── AiException.java
    │   │   ├── OpenRouterService.java     OpenRouter-Chat-Completions-API (HTTP)
    │   │   ├── TutorService.java        System-Prompt, Lernplan, Review, Hinweise
    │   │   ├── FallbackPlanFactory.java Standard-Lernplan ohne API
    │   │   ├── ProgressStorageService.java  Fortschritt als JSON
    │   │   └── CodeRunnerService.java   kompiliert + führt deinen Code lokal (mit Timeout)
    │   └── ui/                      JavaFX-Oberfläche
    │       ├── App.java             JavaFX-Application
    │       ├── WelcomeWizard.java   Willkommensfenster (Name, Vorkenntnisse)
    │       ├── MainView.java        Hauptfenster (Seitenleiste, Tabs)
    │       ├── LessonView.java      Lektion, Editor, Prüfung, Hinweise
    │       ├── ChatView.java        Chat mit dem Tutor
    │       └── JavaSyntaxHighlighter.java  Java-Syntax-Highlighting
    └── resources/
        └── styles.css               Dark-Theme der Oberfläche
```

## 11. Datenschutz

Deine Namen, Fragen und Code werden an die konfigurierte KI-API (Standard: OpenRouter und der dort im Feld `model` gewählte Anbieter) gesendet,
damit der Tutor antworten kann. Lokal gespeichert wird nur die genannte JSON-Datei.
