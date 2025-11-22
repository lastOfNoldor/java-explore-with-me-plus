FROM openjdk:17-slim
COPY target/stat-server-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]