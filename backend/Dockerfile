FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:resolve
COPY src ./src
RUN mvn -B clean package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache fontconfig ttf-dejavu && addgroup -S gmao && adduser -S gmao -G gmao
USER gmao
WORKDIR /app
COPY --from=build /app/target/clinique-gmao-backend-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-Djava.awt.headless=true","-jar","app.jar"]
