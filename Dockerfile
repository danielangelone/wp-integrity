FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build /app/target/wp-integrity.jar /app.jar
USER nobody
ENTRYPOINT ["java", "-jar", "/app.jar"]
