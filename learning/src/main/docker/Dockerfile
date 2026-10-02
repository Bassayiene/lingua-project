# Contexte de build : le dossier learning/
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget tzdata
WORKDIR /app
COPY --from=build /build/target/learning-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx384m", "-XX:MaxMetaspaceSize=192m", "-jar", "app.jar"]
