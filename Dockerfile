FROM maven:3.9-eclipse-temurin-21 AS build
#<repository>:<version>-<variant>
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN useradd --system --create-home appuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app


USER appuser

ENTRYPOINT ["java","-jar","app.jar"]