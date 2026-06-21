@echo off
cd /d "C:\Users\andre\OneDrive\Documentos\alarmes"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=C:\Users\andre\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo  BJGU — Build APK
echo ========================================
echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_HOME=%ANDROID_HOME%
echo.

call gradlew.bat assembleDebug
echo.
echo ========================================
echo  BUILD FINISHED (exit code: %ERRORLEVEL%)
echo ========================================
pause
