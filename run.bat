@echo off
setlocal

echo ===================================================
echo Starting PaathAI - Single Implementation Environment
echo ===================================================

echo.
echo [1/3] Building the backend with Maven...
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo Maven build failed. Exiting...
    pause
    exit /b %errorlevel%
)

echo.
echo [2/3] Starting backend and database services via Docker...
call docker compose up -d --build
if %errorlevel% neq 0 (
    echo Docker Compose failed. Make sure Docker Desktop is running.
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] Starting frontend server...
start "PaathAI Frontend" cmd /k "cd frontend && npx -y serve -p 3000"

echo.
echo ===================================================
echo PaathAI is now successfully running!
echo ===================================================
echo Backend API : http://localhost:8080/api
echo Frontend UI : http://localhost:3000
echo.
echo Press any key to stop all services...
pause >nul

echo.
echo Stopping backend services...
call docker compose down
echo Done. You may close the frontend window manually.
pause
