@ECHO OFF
SETLOCAL
SET BASEDIR=%~dp0
SET WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%BASEDIR%\.mvn\wrapper\maven-wrapper.properties
IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO Error: %WRAPPER_PROPERTIES% is missing.
  EXIT /B 1
)
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Downloading Maven Wrapper jar...
  FOR /F "tokens=2 delims==" %%A IN ('findstr /R "^wrapperUrl=" "%WRAPPER_PROPERTIES%"') DO SET WRAPPER_URL=%%A
  powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')" || (
    ECHO Failed to download Maven Wrapper jar.
    EXIT /B 1
  )
)
SET JAVA_CMD=java
IF NOT "%JAVA_HOME%"=="" SET JAVA_CMD=%JAVA_HOME%\bin\java
"%JAVA_CMD%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
