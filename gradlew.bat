@echo off
setlocal
set "APP_HOME=%~dp0"
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
    powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_JAR%'"
    if errorlevel 1 exit /b 1
)
java -cp "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
