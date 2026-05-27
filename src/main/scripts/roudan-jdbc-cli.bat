@echo off
set DIR=%~dp0
if exist "%DIR%lib\roudan-jdbc-cli.jar" (
    java -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
) else (
    echo roudan: jar not found. Download from https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest
    exit /b 1
)
