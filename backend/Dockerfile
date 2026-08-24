# 后端运行镜像
# 基于 maven 镜像构建，并在此镜像内通过 Playwright CLI 安装 Chromium 浏览器。
# 避免拉取 2GB+ 的 mcr.microsoft.com/playwright/java 官方镜像。
FROM maven:3.9.9-eclipse-temurin-17-focal

WORKDIR /app

# 使用阿里云 Maven 镜像，加速国内依赖下载
RUN mkdir -p /root/.m2 && \
cat > /root/.m2/settings.xml <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF

# 拷贝源码并构建 jar；同时安装 Playwright 浏览器及系统依赖
COPY pom.xml .
COPY src ./src

# 设置 Playwright 浏览器下载镜像，加速国内下载
ENV PLAYWRIGHT_DOWNLOAD_HOST=https://cdn.npmmirror.com/binaries/playwright
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright

# 使用阿里云 apt 镜像，加速国内系统依赖下载（archive.ubuntu.com 在国内经常超时）
RUN sed -i 's|http://archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g; s|http://security.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' /etc/apt/sources.list

# 安装 CJK 中文字体，否则 Playwright 导出的 PDF 中文会显示为方块（豆腐块）
RUN apt-get update && \
    apt-get install -y --no-install-recommends fonts-noto-cjk && \
    rm -rf /var/lib/apt/lists/*

RUN mvn -B clean package -DskipTests && \
    mvn -B exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" && \
    rm -rf /root/.m2/repository /app/src /app/pom.xml /app/target/*.original

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/resume-generation-0.0.1-SNAPSHOT.jar"]
