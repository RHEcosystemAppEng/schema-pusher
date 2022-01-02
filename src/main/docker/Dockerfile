FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.10-6
USER root
RUN microdnf install gzip \
    && microdnf update \
    && microdnf clean all
WORKDIR /app
COPY schema-pusher/target/schema-pusher-jar-with-dependencies.jar \
    schema-pusher/entrypoint.sh \
    schema-pusher/LICENSE ./
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
