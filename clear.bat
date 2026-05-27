@echo off
echo =====================================
echo   roudan-jdbc-cli Cleaner
echo =====================================
echo.

set CLEAR_CONN=n
set /p CLEAR_CONN="Clear saved connections (~/.roudan)? (y/N): "
echo.

echo [1/4] Uninstalling npm package...
call npm uninstall -g roudan-jdbc-cli 2>nul
echo       done.

echo [2/4] Removing CLI install dir...
if exist "%USERPROFILE%\.roudan-cli" (
    rmdir /s /q "%USERPROFILE%\.roudan-cli" 2>nul
    echo       removed: %USERPROFILE%\.roudan-cli
) else (
    echo       not found
)

echo [3/4] Connection data...
if /i "%CLEAR_CONN%"=="y" (
    if exist "%USERPROFILE%\.roudan" (
        rmdir /s /q "%USERPROFILE%\.roudan" 2>nul
        echo       removed: %USERPROFILE%\.roudan
    ) else (
        echo       not found
    )
) else (
    echo       kept: %USERPROFILE%\.roudan
)

echo [4/4] Removing OpenCode skill...
if exist "%USERPROFILE%\.agents\skills\roudan-jdbc" (
    rmdir /s /q "%USERPROFILE%\.agents\skills\roudan-jdbc" 2>nul
    echo       removed
) else (
    echo       not found
)

REM Clean npm bin leftovers
if exist "%APPDATA%\npm" (
    del "%APPDATA%\npm\roudan*" 2>nul
)
if defined NVM_HOME (
    if exist "%NVM_HOME%" (
        del "%NVM_HOME%\roudan*" 2>nul
    )
)

echo.
echo =====================================
echo   Done. Run: npm install -g roudan-jdbc-cli
echo =====================================
pause