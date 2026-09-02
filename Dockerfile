FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN useradd --system --uid 10001 appuser && chown appuser:appuser /app/app.jar
USER 10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
