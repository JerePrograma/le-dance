FROM maven:3.9.10-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
ENV LEDANCE_HOME=/workspace
RUN mvn -B -ntp clean package

FROM eclipse-temurin:21.0.7_6-jre-alpine

RUN addgroup -S ledance && adduser -S ledance -G ledance
WORKDIR /app
RUN mkdir -p /app/data/receipts /app/imgs && chown -R ledance:ledance /app

COPY --from=build --chown=ledance:ledance /workspace/target/backend-1.0.jar app.jar
COPY --from=build --chown=ledance:ledance /workspace/src/main/resources/firma_mesa-de-trabajo-1.png /app/imgs/firma_mesa-de-trabajo-1.png

ENV LEDANCE_HOME=/app \
    APP_RECEIPTS_PATH=/app/data/receipts

USER ledance
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD nc -z 127.0.0.1 8080 || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
