#!/bin/bash

# ============================================
# 飞书妙记后端服务启动脚本
# ============================================

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 项目目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  飞书妙记后端服务启动脚本${NC}"
echo -e "${GREEN}============================================${NC}"

# 检查 Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到 Java，请先安装 JDK 17+${NC}"
    exit 1
fi

java -version 2>&1 | head -1

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}警告: 未找到 Maven，将尝试使用 mvnw${NC}"
    if [ ! -f "./mvnw" ]; then
        echo -e "${RED}错误: 未找到 Maven，请先安装 Maven 或创建 mvnw${NC}"
        exit 1
    fi
    MVN_CMD="./mvnw"
else
    MVN_CMD="mvn"
fi

# 检查 Whisper 服务
echo -e "\n${YELLOW}检查 Whisper 服务 (localhost:5000)...${NC}"
if curl -s --connect-timeout 2 http://localhost:5000/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Whisper 服务正常${NC}"
else
    echo -e "${YELLOW}⚠ Whisper 服务未启动或不可用${NC}"
fi

# 检查 Ollama 服务
echo -e "\n${YELLOW}检查 Ollama 服务 (localhost:11434)...${NC}"
if curl -s --connect-timeout 2 http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Ollama 服务正常${NC}"
else
    echo -e "${YELLOW}⚠ Ollama 服务未启动或不可用${NC}"
fi

# 构建项目
echo -e "\n${YELLOW}正在构建项目...${NC}"
$MVN_CMD clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo -e "${RED}构建失败，请检查错误信息${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 构建成功${NC}"

# 启动服务
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}错误: 未找到 JAR 文件${NC}"
    exit 1
fi

echo -e "\n${GREEN}启动服务: $JAR_FILE${NC}"
echo -e "${GREEN}访问地址: http://localhost:8080/api${NC}"
echo -e "${GREEN}健康检查: http://localhost:8080/api/health${NC}"
echo -e "${GREEN}===========================================${NC}\n"

java -jar "$JAR_FILE" --spring.profiles.active=dev
