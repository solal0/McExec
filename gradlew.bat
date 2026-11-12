@echo off
rem McExec lightweight, source-included Gradle bootstrap.
setlocal
set "APP_HOME=%~dp0"
cd /d "%APP_HOME%"
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -cp "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
