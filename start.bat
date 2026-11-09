@echo off
title Progle Starter
cd /d "%~dp0"
setlocal

set "PROGLE_JDK=F:\my_programs\jdk-21"
if exist "%PROGLE_JDK%\bin\javac.exe" (
    set "JAVA_HOME=%PROGLE_JDK%"
) else (
    echo Hinweis: JDK 21 nicht gefunden unter %PROGLE_JDK% - es wird das systemweite Java versucht.
)

set "MVN=mvn"
where mvn >nul 2>nul
if errorlevel 1 (
    if exist "F:\my_programs\maven\bin\mvn.cmd" (
        set "MVN=F:\my_programs\maven\bin\mvn.cmd"
    ) else (
        echo Maven wurde nicht gefunden. Bitte siehe README.md.
        pause
        exit /b 1
    )
)

if not exist "config.json" (
    if not exist "%USERPROFILE%\.progle\config.json" (
        echo Hinweis: Keine config.json gefunden - Chat und Pruefung brauchen einen API-Key.
        echo Siehe README.md Abschnitt 2.
        echo.
    )
)

echo ============================================================
echo  Progle wird gestartet ...
echo ============================================================
echo.
echo  - Das Progle-Fenster oeffnet sich in ca. 10-60 Sekunden
echo    automatisch. Dieses Fenster schliesst sich von selbst.
echo.
echo  - In der Taskleiste siehst du waehrend des Starts den
echo    Eintrag "Progle-Start" - dort sieht man Details, wenn
echo    etwas nicht klappt. Bitte dieses Fenster NICHT schliessen.
echo.
echo  - Falls du doch im schwarzen Fenster klickst und es
echo    angehalten aussieht: klicken + ENTER druecken.
echo.

start "Progle-Start (NICHT schliessen)" /min cmd /c "call %MVN% javafx:run & if errorlevel 1 (echo. & echo ES IST EIN FEHLER AUFGETRETEN - BITTE TEXT OBEN LESEN. & pause)"

echo Der Start laeuft jetzt im Hintergrund. Dieses Fenster kann
echo geschlossen werden.
timeout /t 6 >nul
endlocal
exit
