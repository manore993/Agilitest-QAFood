CLS
@ECHO off

cd /D "%~dp0"

SET "ATS_JDK_LIGHT_URL=https://actiontestscript.org/tools/jdk-light/windows/jdk.zip"

SET "JAVA_TOOL_OPTIONS="
SET "_JAVA_OPTIONS="

SET "ats_folder=%APPDATA%\ats"
SET "ats_tools=%ats_folder%\tools"

SET "clean_arg="
SET "suites_arg= -suiteXmlFiles="
SET "report_arg="
SET "valid_arg="

IF not exist %ats_folder% (mkdir %ats_folder%)
IF not exist %ats_tools% (mkdir %ats_tools%)

:continue
IF "%1"=="" GOTO end
IF %1 == clean (
	SET "clean_arg=clean"
	powershell -command "Remove-Item -recurse -Path \"%ats_tools%\jdk-light\""
)

IF %1 == valid (
	SET "valid_arg= -validationReport=1"
)

SET "test_xml=%1"

IF "%test_xml:~-4%"==".xml"	SET suites_arg=%suites_arg%%test_xml%,
IF "%test_xml:~0,12%"=="exec-report-" SET report_arg= -reportLevel=%test_xml:~-1%

SHIFT
GOTO continue
:end

SET java_exec=%ats_tools%\jdk-light\bin\java.exe

SET command_line=%clean_arg%%suites_arg:~0,-1%%report_arg%%valid_arg%

:CHECK_JDK
IF NOT EXIST %java_exec% (
    SET "ATS_DOWNLOAD_JDK_LIGHT=1"
    GOTO SUITE
)

SET "ATS_DOWNLOAD_JDK_LIGHT=0"
GOTO SUITE

:SUITE
IF "%ATS_DOWNLOAD_JDK_LIGHT%"=="1" (
  IF EXIST %ats_tools%\jdk-light\NUL (
	ECHO [AGILITEST] Delete folder : %ats_tools%\jdk-light
    powershell -command "Remove-Item -recurse -Path \"%ats_tools%\jdk-light\""
  )

  ECHO [AGILITEST] Start download jdk-light: %ATS_JDK_LIGHT_URL%
  powershell -command "Set-Variable ProgressPreference SilentlyContinue;Invoke-WebRequest -Uri \"%ATS_JDK_LIGHT_URL%\" -OutFile \"%ats_tools%\jdk-light.zip\""
  
  ECHO [AGILITEST] Expand jdk-light archive folder: %ats_tools%\jdk-light
  powershell -command "Set-Variable ProgressPreference SilentlyContinue;Expand-Archive \"%ats_tools%\jdk-light.zip\" -DestinationPath \"%ats_tools%\jdk-light""
  
  ECHO [AGILITEST] Delete archive file: %ats_tools%\jdk-light.zip
  powershell -command "Remove-Item -Path \"%ats_tools%\jdk-light.zip\""
  
  ECHO [AGILITEST] jdk-light installed, execute AtsLauncher script ...
)

IF not exist %java_exec% (
  SET java_exec="java"
)
rem Unset variable
SET "ats_folder="
SET "ats_tools="

%java_exec% AtsLauncher.java %command_line%
