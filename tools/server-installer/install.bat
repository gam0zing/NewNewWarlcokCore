@echo off
chcp 65001 >nul
cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo [错误] 未检测到 Node.js。
  echo 请先安装 Node.js 18 或更高版本：https://nodejs.org/
  echo 安装完成后重新双击本文件。
  pause
  exit /b 1
)

node install.js %*
if errorlevel 1 (
  echo.
  echo 安装未完成，请查看上面的错误信息。
)

echo.
pause
