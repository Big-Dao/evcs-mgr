FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*

COPY evcs-gateway/build/libs/evcs-gateway-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail http://localhost:8080/actuator/health || exit 1

# 支持通过环境变量注入 JVM 与 Spring 配置（包含 -Dspring.config.import 等）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
