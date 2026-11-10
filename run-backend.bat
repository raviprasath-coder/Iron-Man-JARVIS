@echo off
echo ========================================================
echo   Starting J.A.R.V.I.S. Spring Boot Backend on Port 8080
echo ========================================================
cd /d "%~dp0Backend\jarvis-backend"
call mvnw.cmd spring-boot:run
pause
