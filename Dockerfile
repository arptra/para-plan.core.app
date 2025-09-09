FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar && mv $(find build/libs -name '*.jar' ! -name '*-plain.jar') app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

