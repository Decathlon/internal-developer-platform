FROM eclipse-temurin:25-jre-alpine@sha256:f96a37c0896c613b4328e0e54d6fad5a2e60061cff878a7cca2be10358a6e77d

ENV PORT=8080

RUN apk add --no-cache shadow && \
    groupadd -r idp-core-group && \
    useradd --no-log-init -r -g idp-core-group idp-core-user && \
    apk del shadow

USER idp-core-user:idp-core-group

WORKDIR /opt
COPY ./target/idp-core*.jar /opt/app.jar

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
