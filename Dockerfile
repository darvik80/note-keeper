# Этап 1: Сборка приложения
FROM maven:3.9.16-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
COPY note-keeper-service/pom.xml note-keeper-service/
COPY note-keeper-web/pom.xml note-keeper-web/

COPY note-keeper-service/src note-keeper-service/src
COPY note-keeper-web/src note-keeper-web/src
COPY note-keeper-web/index.html note-keeper-web/
COPY note-keeper-web/package.json note-keeper-web/
COPY note-keeper-web/*.json note-keeper-web/

RUN mvn clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/note-keeper-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]