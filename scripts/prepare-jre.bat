@echo off
REM prepare-jre.bat — download JRE for release assets
REM Usage: prepare-jre.bat <version>
REM Outputs: public/roudan-jre8-*.zip ready for gh release upload

set VERSION=%1
if "%VERSION%"=="" set VERSION=0.5.2

echo Downloading JRE 8 for Windows x64...
curl -fL -o "public\roudan-jre8-windows-x64.zip" "https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"
if errorlevel 1 exit /b 1

echo Done. Upload with:
echo   gh release upload v%VERSION% public\roudan-jre8-windows-x64.zip public\roudan-jre8-linux-x64.tar.gz public\roudan-jre8-mac-x64.tar.gz
