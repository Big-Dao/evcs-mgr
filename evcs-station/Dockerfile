FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
# Build dependencies first (evcs-common and evcs-protocol)
RUN ./gradlew :evcs-common:jar :evcs-protocol:jar --no-daemon
# Then build the station service
RUN ./gradlew :evcs-station:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /workspace/evcs-station/build/libs/evcs-station-*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ENV JAVA_OPTS=""
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl --fail http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
