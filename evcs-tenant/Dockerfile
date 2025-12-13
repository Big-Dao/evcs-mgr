FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk add --no-cache curl && \
    rm -rf /var/cache/apk/*
COPY evcs-tenant/build/libs/evcs-tenant-*.jar app.jar
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
