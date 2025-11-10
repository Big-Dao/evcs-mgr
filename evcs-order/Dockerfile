FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*
COPY evcs-order/build/libs/evcs-order-*.jar app.jar
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
