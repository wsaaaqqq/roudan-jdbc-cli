@echo off
REM roudan-jdbc-cli Database Test Script
REM Usage: test_db_all.bat
REM Requires Docker containers running on appropriate ports

set JAR=target\roudan-jdbc-cli.jar
if not exist %JAR% (
    echo Building jar first...
    call mvn package -DskipTests -q
)

echo ========================================
echo  Database Connectivity Tests
echo ========================================

setlocal enabledelayedexpansion
set PASS=0
set FAIL=0
set SKIP=0

call :test "H2 in-memory"       "java -jar %JAR% -u jdbc:h2:mem:test -n sa test --timeout 10000"
call :test "MySQL 8.0"          "java -jar %JAR% -u jdbc:mysql://127.0.0.1:33060/test -n root -p root test --timeout 10000"
call :test "PostgreSQL 15"      "java -jar %JAR% -u jdbc:postgresql://127.0.0.1:54320/test -n postgres -p postgres test --timeout 10000"
call :test "MariaDB 11"         "java -jar %JAR% -u jdbc:mariadb://127.0.0.1:33070/test -n root -p root test --timeout 10000"
call :test "SQL Server 2022"    "java -jar %JAR% -u jdbc:sqlserver://127.0.0.1:14330;trustServerCertificate=true -n sa -p Test123! test --timeout 10000"
call :test "Oracle 21c XE"      "java -jar %JAR% -u jdbc:oracle:thin:@127.0.0.1:15210/XEPDB1 -n test -p test123 test --timeout 10000"
call :test "DM7"                "java -jar %JAR% -u jdbc:dm://127.0.0.1:5236 -n SYSDBA -p Nice_2016 test --timeout 10000"

echo ========================================
echo  Results: %PASS% passed, %FAIL% failed, %SKIP% skipped
echo ========================================
exit /b %FAIL%

:test
    set NAME=%~1
    set CMD=%~2
    echo [TEST] %NAME%...
    %CMD% >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        echo   PASS
        set /a PASS+=1
    ) else (
        echo   FAIL
        set /a FAIL+=1
    )
    goto :eof
