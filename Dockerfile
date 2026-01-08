FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.8.2_2.13.10 AS builder

WORKDIR /build

COPY build.sbt .
COPY project/ project/

RUN sbt update
COPY src/ src/

RUN sbt assembly

# Runtime
FROM eclipse-temurin:11-jre-jammy

WORKDIR /app
COPY --from=builder /build/target/scala-2.13/ComicBot-assembly-0.1.jar /app/bot.jar

ENTRYPOINT ["java", "-jar", "/app/bot.jar"]
