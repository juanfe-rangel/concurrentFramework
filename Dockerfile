FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:resolve

COPY src src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/classes ./classes

ENV PORT=35000

EXPOSE 35000

ENTRYPOINT ["java", "-cp", "classes", "org.example.MicroSpringboot"]
