@echo off
echo ===================================================
echo   Compiling Secure Banking Application (Java)...
echo ===================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src\com\securebank\model\*.java src\com\securebank\security\*.java src\com\securebank\repository\*.java src\com\securebank\service\*.java src\com\securebank\ui\*.java src\com\securebank\Main.java

if %ERRORLEVEL% EQU 0 (
    echo [OK] Compilation successful! Output stored in bin\
) else (
    echo [FAIL] Compilation failed with error code %ERRORLEVEL%
)
