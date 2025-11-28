FROM eclipse-temurin:21-jre-jammy
RUN mkdir /app
COPY ./target/main-service-0.0.1-SNAPSHOT.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]