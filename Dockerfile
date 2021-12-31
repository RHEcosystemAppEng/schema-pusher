FROM registry.access.redhat.com/ubi8/openjdk-17:1.10-5 AS build
USER root
RUN microdnf install gzip \
    && microdnf update \
    && microdnf clean all
WORKDIR /sources
RUN curl https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz \
    --output apache-maven-bin.tar.gz \
    && tar xzf apache-maven-bin.tar.gz
COPY src/ pom.xml ./
RUN ./apache-maven-3.8.4/bin/mvn package -Ppkg -B -ff

FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.10-6 AS runtime
USER root
RUN microdnf install gzip \
    && microdnf update \
    && microdnf clean all
WORKDIR /app
COPY --from=build /sources/target/schema-pusher-jar-with-dependencies.jar ./schema-pusher.jar
COPY entrypoint.sh LICENSE ./
RUN chmod a+x entrypoint.sh && \
    chown -R 1001:0 .
USER 1001
ENTRYPOINT ["/app/entrypoint.sh"]

ARG BUILD_DATE
ARG VERSION

LABEL org.opencontainers.image.created=$BUILD_DATE \
org.opencontainers.image.authors="Ecosystem Enginerring Team, Red Hat" \
org.opencontainers.image.url="TODO add image url" \
org.opencontainers.image.version=$VERSION \
org.opencontainers.image.vendor="Red Hat" \
org.opencontainers.image.licenses="MIT" \
org.opencontainers.image.title="Schema Pusher" \
org.opencontainers.image.description="Use for pushing schemas to Red Hat's Service Registry"
