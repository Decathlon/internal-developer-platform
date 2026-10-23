FROM eclipse-temurin:25-jre-alpine

ENV PORT=8080

RUN addgroup -S idp-core-group && \
    adduser -S -D -H -G idp-core-group idp-core-user

USER idp-core-user:idp-core-group

WORKDIR /opt
COPY ./target/idp-core*.jar /opt/app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
    CMD wget -q --spider "http://localhost:${SERVER_PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
