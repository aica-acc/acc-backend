# syntax=docker/dockerfile:1
# Spring Boot 3.4.10 / Java 17 (pom.xml 기준)
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
 && cp target/acc-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:17.0.17_10-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8081
# application.properties 는 레포에 없으므로 모든 설정은 환경변수(SPRING_*, REACT_BASE_PATH 등)로 주입한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
