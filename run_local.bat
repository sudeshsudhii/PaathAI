@echo off
setlocal

echo ===================================================
echo Starting PaathAI - Native Environment (No Docker)
echo ===================================================

echo.
echo [1/2] Starting backend API...
start "PaathAI Backend" cmd /k "mvnw spring-boot:run -pl paathai-app -Dspring-boot.run.profiles=dev"

echo.
echo [2/2] Starting frontend server...
start "PaathAI Frontend" cmd /k "cd frontend && npx -y serve -p 3000"

echo.
echo ===================================================
echo PaathAI native services have been launched in separate windows!
echo ===================================================
echo Note: The backend will crash if PostgreSQL (with pgvector) is not running on port 5432.
echo Note: Audio transcription will fail if Whisper ASR is not running on port 9000.
echo.
pause
