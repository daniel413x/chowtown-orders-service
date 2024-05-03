# Use the Maven image with OpenJDK to build the project
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests && \
    mv target/*.jar app.jar && \
    mv app.jar /tmp/

FROM openjdk:17-slim
WORKDIR /app+
COPY --from=build /tmp/ ./
EXPOSE 5002
CMD ["java", "-jar", "app.jar"]
