@echo off
chcp 65001 >nul
echo =====================================
echo   roudan-jdbc-cli 清理脚本
echo =====================================
echo.

REM 1. 询问是否清除数据库连接信息
set CLEAR_CONN=n
set /p CLEAR_CONN="清除已保存的数据库连接信息? (y/N): "
echo.

REM 2. npm 卸载
echo [1/4] 卸载 npm 全局包...
call npm uninstall -g roudan-jdbc-cli 2>nul
echo       done.

REM 3. CLI 安装目录
echo [2/4] 删除 CLI 安装目录...
if exist "%USERPROFILE%\.roudan-cli" (
    rmdir /s /q "%USERPROFILE%\.roudan-cli" 2>nul
    echo       removed: %USERPROFILE%\.roudan-cli
) else (
    echo       not found, skip
)

REM 4. 连接信息 (按用户选择)
echo [3/4] 连接信息...
if /i "%CLEAR_CONN%"=="y" (
    if exist "%USERPROFILE%\.roudan" (
        rmdir /s /q "%USERPROFILE%\.roudan" 2>nul
        echo       removed: %USERPROFILE%\.roudan
    ) else (
        echo       not found, skip
    )
) else (
    echo       kept (已保留 %USERPROFILE%\.roudan)
)

REM 5. OpenCode skill
echo [4/4] 删除 OpenCode skill...
if exist "%USERPROFILE%\.agents\skills\roudan-jdbc" (
    rmdir /s /q "%USERPROFILE%\.agents\skills\roudan-jdbc" 2>nul
    echo       removed: roudan-jdbc skill
) else (
    echo       not found, skip
)

REM 6. npm bin 残留
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
echo   清理完成
echo =====================================
echo.
if /i not "%CLEAR_CONN%"=="y" (
    echo 连接信息已保留在: %USERPROFILE%\.roudan
)
echo 现在可以执行: npm install -g roudan-jdbc-cli
echo.
pause
