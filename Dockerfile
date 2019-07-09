FROM maven:3.6.1-jdk-11 AS builder
WORKDIR /app
ADD ./src ./src
ADD pom.xml .
RUN mvn clean verify

FROM adoptopenjdk/openjdk11:jre-11.0.3_7-alpine
WORKDIR /app
COPY --from=builder /app/target/spring-boot-pre-authentication*.jar app.jar
EXPOSE 8080
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]