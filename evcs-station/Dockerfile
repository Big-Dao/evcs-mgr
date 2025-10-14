# 运行阶段 - 使用预构建的JAR文件
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs && chmod 777 /app/logs

# 创建非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 复制预构建的jar文件 (使用boot jar)
COPY evcs-station/build/libs/evcs-station-*-boot.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# 暴露端口
EXPOSE 8082

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.cloud.config.enabled=false", \
    "-Dspring.cloud.discovery.enabled=false", \
    "-jar", \
    "app.jar"]
