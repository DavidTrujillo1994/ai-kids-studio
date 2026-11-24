@echo off
chcp 65001 >nul
title AI Kids Studio - no cierres esta ventana
cd /d "%~dp0"
set "URL=http://127.0.0.1:8095"
set "JAR=backend\ai-kids-backend\bootstrap\build\libs\ai-kids-studio.jar"
set "WEB=frontend\angular-app\dist\angular-app\browser\index.html"

curl.exe -s -f -o nul %URL%/api/v1/series && (
  start "" %URL%
  exit /b 0
)

where java >nul 2>nul || (
  echo Falta Java. Instala Java 21 o superior y vuelve a intentarlo.
  pause
  exit /b 1
)
if not exist "%JAR%" (
  echo Preparando la aplicacion por primera vez...
  call backend\ai-kids-backend\gradlew.bat -p backend\ai-kids-backend bootJar --console=plain || (pause & exit /b 1)
)
if not exist "%WEB%" (
  echo Preparando las pantallas por primera vez...
  pushd frontend\angular-app
  call npm install --no-audit --no-fund || (popd & pause & exit /b 1)
  call npx ng build || (popd & pause & exit /b 1)
  popd
)

echo.
echo   AI Kids Studio se esta abriendo en tu navegador: %URL%
echo   Deja esta ventana abierta mientras lo uses. Para salir, cierrala.
echo.
start "" /b powershell -NoProfile -WindowStyle Hidden -Command "for($i=0;$i -lt 90;$i++){ try{ Invoke-WebRequest -UseBasicParsing %URL%/api/v1/series -TimeoutSec 2 ^| Out-Null; Start-Process %URL%; break }catch{ Start-Sleep 1 } }"
java -Xmx320m -Dfile.encoding=UTF-8 -jar "%JAR%"
pause
