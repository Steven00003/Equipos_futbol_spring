@echo off
chcp 65001 >nul
title Relaciones JPA - Club de Futbol

REM Se ubica en la carpeta de este .bat para que la base de datos
REM H2 (./data/relacionesdb) se cree junto a este archivo y al jar.
cd /d "%~dp0"

echo ============================================
echo  Relaciones JPA - Club de Futbol (puerto 8118)
echo ============================================
echo.

REM Comprueba que Java esta disponible
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro Java en este equipo.
    echo Por favor instala Java 17 o superior ^(por ejemplo Oracle JDK u OpenJDK 17 o mayor^)
    echo y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

echo Cerrando esta ventana se detiene la aplicacion.
echo Abriendo el navegador en http://localhost:8118/ en unos segundos...
start "" cmd /c "timeout /t 10 >nul & start "" http://localhost:8118/"

echo Iniciando la aplicacion...
java -jar RelacionesFutbol.jar

REM Si la aplicacion termina sola, deja la ventana abierta para leer el error
echo.
echo La aplicacion termino. Revisa el mensaje anterior si hubo un error.
pause
