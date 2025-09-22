# -------- Dependency stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS deps
WORKDIR /app

# Copy only pom.xml for dependency resolution
COPY pom.xml .

# Download all dependencies into cache (offline mode)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build with cached dependencies (only new deps will download)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup --system appuser && adduser --system --ingroup appuser appuser

# Copy built JAR from build stage
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Environment
ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
