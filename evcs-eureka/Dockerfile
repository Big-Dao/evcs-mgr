FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*

COPY evcs-eureka/build/libs/evcs-eureka*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail http://localhost:8761/ || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
