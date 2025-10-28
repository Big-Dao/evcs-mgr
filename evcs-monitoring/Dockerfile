# Stage 1: Download dependencies (cached layer)
FROM gradle:8.5-jdk21-alpine AS dependencies
WORKDIR /workspace
# Copy only dependency-related files for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
# Download dependencies to cache them (using gradle command from base image)
RUN gradle dependencies --no-daemon || true

# Stage 2: Build application
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
# Copy cached dependencies from previous stage
COPY --from=dependencies /root/.gradle /root/.gradle
# Copy dependency files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
# Copy only necessary source files
COPY evcs-common ./evcs-common
COPY evcs-monitoring ./evcs-monitoring
# Build only this module (using gradle command from base image)
RUN gradle :evcs-monitoring:bootJar --no-daemon --parallel

# Stage 3: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Install curl for health checks only
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*
# Copy only the built jar
COPY --from=build /workspace/evcs-monitoring/build/libs/evcs-monitoring-*.jar app.jar
# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring app.jar
USER spring:spring
# Optimize JVM for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8087
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
