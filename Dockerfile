FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

RUN chmod +x gradlew && \
	for attempt in 1 2 3 4 5; do \
		./gradlew bootJar --no-daemon \
			-Dorg.gradle.internal.http.connectionTimeout=120000 \
			-Dorg.gradle.internal.http.socketTimeout=120000 && exit 0; \
		echo "Gradle build attempt ${attempt} failed; retrying..."; \
	done; \
	exit 1

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN useradd --system --uid 10001 appuser && chown appuser:appuser /app/app.jar
USER 10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
