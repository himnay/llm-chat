# syntax=docker/dockerfile:1

# ---- build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Cache dependencies first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
# Build
COPY src ./src
RUN ./mvnw -q -B -DskipTests clean package

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
# Run as a non-root user
RUN useradd --system --uid 10001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8082
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
