# syntax=docker/dockerfile:1

# Builds one module of the llm-chat reactor (llm-chat-agent | llm-audio | llm-image).
# Usage: docker build --build-arg MODULE=llm-audio --build-arg PORT=8083 -t llm-audio .

# ---- build stage ----
# Java 27 images: eclipse-temurin:27 wasn't on Docker Hub yet (Sept 2026), so these stages use
# SapMachine 27, an OpenJDK build that is also a Docker Official Image.
FROM sapmachine:27-jdk-ubuntu AS build
ARG MODULE=llm-chat-agent
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY llm-chat-agent/pom.xml llm-chat-agent/pom.xml
COPY llm-audio/pom.xml llm-audio/pom.xml
COPY llm-image/pom.xml llm-image/pom.xml
RUN ./mvnw -q -B dependency:go-offline -pl ${MODULE} -am
COPY llm-chat-agent/src llm-chat-agent/src
COPY llm-audio/src llm-audio/src
COPY llm-image/src llm-image/src
RUN ./mvnw -q -B -pl ${MODULE} -am -DskipTests clean package

# ---- layer extraction stage ----
FROM sapmachine:27-jre-ubuntu AS extract
ARG MODULE=llm-chat-agent
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
# Boot 3.3+/4 replaced -Djarmode=layertools with the 'tools' jarmode
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- runtime stage ----
FROM sapmachine:27-jre-ubuntu
ARG PORT=8082
WORKDIR /app
# The SapMachine image has no curl; the HEALTHCHECK below needs it
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# llm-chat-agent's logback config writes logs/llm-chat.json under /app, which the non-root user must be able to create
RUN useradd --system --uid 10001 appuser \
    && mkdir -p /app/logs && chown appuser /app/logs
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
USER appuser
EXPOSE ${PORT}
ENV SERVER_PORT=${PORT}
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT}/ai/actuator/health || exit 1
