@echo off
setlocal enabledelayedexpansion

REM MTG Engine CLI
REM Usage: mtg <command> [args...]

set SCRIPT_DIR=%~dp0

REM Check if project needs to be built
if not exist "%SCRIPT_DIR%mtg-tools\target\classes" (
    echo Building project...
    pushd "%SCRIPT_DIR%"
    call mvnw.cmd compile -q -DskipTests
    popd
)

REM Build module path from all module target directories
set MP=

for %%M in (mtg-engine mtg-tools) do (
    if exist "%SCRIPT_DIR%%%M\target\classes" (
        set MP=!MP!;%SCRIPT_DIR%%%M\target\classes
    )
)

REM Add dependencies from Maven local repository
pushd "%SCRIPT_DIR%"
for /f "tokens=*" %%i in ('call mvnw.cmd -q -pl mtg-tools dependency:build-classpath -Dmdep.outputFile=CON 2^>nul') do set MVN_MP=%%i
popd

set MP=!MP!;!MVN_MP!

REM Remove leading semicolon
set MP=!MP:~1!

REM Run the CLI on the module path
REM --add-modules ALL-MODULE-PATH ensures automatic modules' dependencies are resolved
java -p "%MP%" --add-modules ALL-MODULE-PATH -m be.imgn.mtg.tools/be.imgn.mtg.tooling.Main %*
