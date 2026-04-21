@echo off
REM ============================================
REM MeetingMind后端服务启动脚本 (Windows)
REM ============================================

echo ===========================================
echo   MeetingMind后端服务启动脚本
echo ===========================================

REM 项目目录
set PROJECT_DIR=%~dp0
cd /d %PROJECT_DIR%

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 Java，请先安装 JDK 17+
    pause
    exit /b 1
)

REM 检查 Maven
where mvn >nul 2>&1
if errorlevel 1 (
    echo 警告: 未找到 Maven
    where mvnw >nul 2>&1
    if errorlevel 1 (
        echo 错误: 未找到 Maven，请先安装 Maven
        pause
        exit /b 1
    )
    set MVN_CMD=mvnw
) else (
    set MVN_CMD=mvn
)

REM 检查 Whisper 服务
echo.
echo 检查 Whisper 服务 (localhost:5000)...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:5000/health' -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host 'OK - Whisper 服务正常' -ForegroundColor Green } catch { Write-Host 'WARNING - Whisper 服务未启动或不可用' -ForegroundColor Yellow }"

REM 检查 Ollama 服务
echo.
echo 检查 Ollama 服务 (localhost:11434)...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:11434/api/tags' -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host 'OK - Ollama 服务正常' -ForegroundColor Green } catch { Write-Host 'WARNING - Ollama 服务未启动或不可用' -ForegroundColor Yellow }"

REM 构建项目
echo.
echo 正在构建项目...
call %MVN_CMD% clean package -DskipTests -q

if errorlevel 1 (
    echo 构建失败，请检查错误信息
    pause
    exit /b 1
)

echo 构建成功

REM 查找 JAR 文件
for /r target %%i in (*.jar) do (
    if not "%%i"=="%PROJECT_DIR%target\%%~nxi" (
        set JAR_FILE=%%i
    )
)

if not defined JAR_FILE (
    echo 错误: 未找到 JAR 文件
    pause
    exit /b 1
)

echo.
echo 启动服务: %JAR_FILE%
echo 访问地址: http://localhost:8080/api
echo 健康检查: http://localhost:8080/api/health
echo ===========================================
echo.

java -jar "%JAR_FILE%" --spring.profiles.active=dev

pause
