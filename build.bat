@echo off
setlocal

set "LOG_FILE=%~dp0build.log"

if not defined EE_BUILD_LOGGING (
    set "EE_BUILD_LOGGING=1"
    call "%~f0" %* > "%LOG_FILE%" 2>&1
    set "STATUS=%ERRORLEVEL%"
    type "%LOG_FILE%"
    exit /b %STATUS%
)

cd /d "%~dp0"

set "MAVEN_OPTS=-Dfile.encoding=UTF-8"
set "JAVA_HOME=D:\java\jdk-25"
set "PATH=%JAVA_HOME%\bin;%PATH%"

for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'"') do set "BUILD_STARTED=%%i"

echo [INFO] Build log: %LOG_FILE%
echo [INFO] Started at: %BUILD_STARTED%
echo.

where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven not found. Please install Maven and add it to PATH.
    exit /b 1
)
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] JDK not found. Please install JDK and add it to PATH.
    exit /b 1
)

echo [INFO] Detect JDK Version:
java -version
echo.

echo [INFO] Cleaning previous build...
call mvn clean %*
if %ERRORLEVEL% neq 0 goto :fail
echo.

echo [INFO] Building ExcellentEnchants...
call mvn package -DskipTests %*
if %ERRORLEVEL% neq 0 goto :fail

echo.
echo [OK] Build complete!
for /f "delims=" %%i in ('dir /b /s target\*.jar 2^>nul') do echo [OK] Artifact: %%i
for /f "delims=" %%i in ('dir /b /s Core\target\*.jar 2^>nul') do echo [OK] Artifact: %%i
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'"') do set "BUILD_FINISHED=%%i"
echo [INFO] Finished at: %BUILD_FINISHED%
exit /b 0

:fail
echo [ERROR] Build failed.
exit /b 1
