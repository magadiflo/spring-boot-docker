FROM maven:3.9.3 AS build
WORKDIR /app
ARG SERVER_PORT
COPY pom.xml /app
RUN mvn dependency:resolve
COPY . /app
RUN mvn clean
RUN mvn package -DskipTests -X

FROM openjdk:17-jdk-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE ${SERVER_PORT}
CMD["java", "-jar", "app.jar"]