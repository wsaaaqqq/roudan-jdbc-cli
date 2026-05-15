@echo off
set DIR=%~dp0
set JAVA=%DIR%jre8\bin\java.exe
if exist "%JAVA%" (
    "%JAVA%" -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
) else (
    java -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
)
