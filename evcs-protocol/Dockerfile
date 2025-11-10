FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*
# 仅拷贝可执行的 Spring Boot JAR（带 -boot 分类）
COPY evcs-protocol/build/libs/evcs-protocol-*-boot.jar app.jar
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
