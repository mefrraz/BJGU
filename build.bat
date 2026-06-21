@echo off
cd /d "C:\Users\andre\OneDrive\Documentos\alarmes"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=C:\Users\andre\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo  BJGU -- Build APK
echo ========================================
echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_HOME=%ANDROID_HOME%
echo.

call gradlew.bat assembleDebug

echo.
echo ========================================
if %ERRORLEVEL% equ 0 (
    echo  BUILD SUCCESSFUL
    echo  APK: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo  BUILD FAILED (exit code: %ERRORLEVEL%)
)
echo ========================================
