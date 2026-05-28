FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY paathai-app/target/paathai-app-*.jar app.jar
COPY prompts/ prompts/

RUN mkdir -p /app/audio

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
