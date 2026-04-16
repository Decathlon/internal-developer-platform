FROM eclipse-temurin:25-jre-alpine@sha256:f10d6259d0798c1e12179b6bf3b63cea0d6843f7b09c9f9c9c422c50e44379ec

ENV PORT=8080

RUN apk add --no-cache shadow && \
    groupadd -r idp-core-group && \
    useradd --no-log-init -r -g idp-core-group idp-core-user && \
    apk del shadow

USER idp-core-user:idp-core-group

WORKDIR /opt
COPY ./target/idp-core*.jar /opt/app.jar

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
