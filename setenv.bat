@echo off
REM setenv.bat — Configura as variáveis de ambiente para compilar o BJGU
REM
REM Executa este script antes de correres o gradlew.bat:
REM   C:\Users\andre\OneDrive\Documentos\alarmes> setenv.bat
REM   C:\Users\andre\OneDrive\Documentos\alarmes> gradlew.bat assembleDebug
REM
REM Ou corre diretamente com:
REM   cmd /c "setenv.bat && gradlew.bat assembleDebug"

REM JDK do Android Studio (JetBrains Runtime 21)
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

REM Android SDK
set ANDROID_HOME=C:\Users\andre\AppData\Local\Android\Sdk

REM Adicionar ao PATH
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_HOME=%ANDROID_HOME%
echo.
echo Ambiente configurado. Corre: gradlew.bat assembleDebug
