@echo off
echo ===================================================
echo   Starting Secure Banking Application...
echo ===================================================

if not exist bin\com\securebank\Main.class (
    echo Binaries not found. Building project first...
    call build.bat
)

java -cp bin com.securebank.Main
pause
