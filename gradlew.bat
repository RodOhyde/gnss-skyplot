@echo off
if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  java -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
) else (
  gradle %*
)
