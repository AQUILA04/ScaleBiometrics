@echo off
REM ScaleBiometrics - Build All Docker Images (Windows)
REM This script builds all Docker images for the ScaleBiometrics platform

echo ========================================
echo ScaleBiometrics - Building Docker Images
echo ========================================
echo.

set PROJECT_ROOT=%~dp0..\..
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo [1/5] Building Keycloak Image...
docker build -t scalebiometrics/keycloak:latest ^
             -t scalebiometrics/keycloak:%TIMESTAMP% ^
             -f "%PROJECT_ROOT%\infrastructure\keycloak\Dockerfile" ^
             "%PROJECT_ROOT%\infrastructure\keycloak"
if %errorlevel% neq 0 (
    echo ERROR: Keycloak build failed!
    exit /b 1
)
echo Keycloak image built successfully!
echo.

echo [2/5] Building Backend API Image...
docker build -t scalebiometrics/api:latest ^
             -t scalebiometrics/api:%TIMESTAMP% ^
             -f "%PROJECT_ROOT%\apps\api\Dockerfile" ^
             "%PROJECT_ROOT%\apps\api"
if %errorlevel% neq 0 (
    echo ERROR: Backend API build failed!
    exit /b 1
)
echo Backend API image built successfully!
echo.

echo [3/5] Building Frontend Web Image...
docker build -t scalebiometrics/web:latest ^
             -t scalebiometrics/web:%TIMESTAMP% ^
             -f "%PROJECT_ROOT%\apps\web\Dockerfile" ^
             "%PROJECT_ROOT%\apps\web"
if %errorlevel% neq 0 (
    echo ERROR: Frontend Web build failed!
    exit /b 1
)
echo Frontend Web image built successfully!
echo.

echo [4/5] Listing built images...
docker images | findstr scalebiometrics
echo.

echo [5/5] Build Summary
echo ========================================
echo All images built successfully!
echo Timestamp: %TIMESTAMP%
echo.
echo Images:
echo - scalebiometrics/keycloak:latest
echo - scalebiometrics/api:latest
echo - scalebiometrics/web:latest
echo ========================================
echo.

echo To start the services, run:
echo   cd infrastructure\local
echo   docker-compose up -d
echo.

pause
