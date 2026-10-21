# syntax=docker/dockerfile:1
#
# One image for every Spring Boot app of the project (eureka, gateway, 6 services).
# Maven builds the whole reactor ONCE, then each container runs one jar,
# chosen with the APP environment variable (see docker-compose.yml):
#   APP=movie-service  ->  java -jar /app/movie-service.jar
#
# Build alone:  docker build -t mss301-app .

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
# ~/.m2 is cached between builds (BuildKit), so only the first build downloads the dependencies.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -Dmaven.test.skip=true package \
 && mkdir -p /out \
 && for module in infras/eureka-service infras/api-gateway services/*-service; do \
      jar="$(ls "$module"/target/*.jar | grep -v -e '-plain\.jar$' -e '-sources\.jar$' | head -n 1)"; \
      cp "$jar" "/out/$(basename "$module").jar"; \
    done \
 && ls -la /out

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring --uid 1001 spring
COPY --from=build /out/ /app/
USER spring
# Small heaps: ~10 JVMs share one laptop during the demo.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    APP=""
ENTRYPOINT ["sh", "-c", "if [ -z \"$APP\" ]; then echo 'Set APP (e.g. APP=movie-service)'; ls /app; exit 1; fi; exec java $JAVA_OPTS -jar /app/$APP.jar"]
