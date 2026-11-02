FROM eclipse-temurin:25-jre-jammy

ENV SERVER_PORT=8080 \
    PORT=8080

WORKDIR /opt

RUN groupadd -r -g 1001 idp-core-group && \
    useradd -r -u 1001 -g idp-core-group -s /bin/false idp-core-user

COPY ./target/idp-core*.jar /opt/app.jar

USER 1001:1001

HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
    CMD wget -q --spider "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
