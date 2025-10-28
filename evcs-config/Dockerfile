FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
# Build dependencies first (evcs-common)
RUN ./gradlew :evcs-common:jar --no-daemon
# Then build the config service
RUN ./gradlew :evcs-config:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /workspace/evcs-config/build/libs/*.jar app.jar
COPY --from=build /workspace/config-repo /config-repo
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=local
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:/config-repo
EXPOSE 8888
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl --fail http://localhost:8888/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
