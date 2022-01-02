FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.10-6
USER root
RUN microdnf install gzip \
    && microdnf update \
    && microdnf clean all
WORKDIR /app
COPY schema-pusher/target/schema-pusher-jar-with-dependencies.jar \
    schema-pusher/src/main/shell/entrypoint.sh \
    schema-pusher/LICENSE ./
RUN chmod a+x entrypoint.sh && \
    chown -R 1001:0 .
USER 1001
ENTRYPOINT ["/app/entrypoint.sh"]

LABEL org.opencontainers.image.created="@git.build.time@" \
org.opencontainers.image.authors="Ecosystem Engineering Team, Red Hat" \
org.opencontainers.image.url="TODO add image url" \
org.opencontainers.image.source="TODO add source url" \
org.opencontainers.image.documentation="TODO add docs url" \
org.opencontainers.image.version="@git.build.version@" \
org.opencontainers.image.revision="@git.commit.id.full@" \
org.opencontainers.image.ref.name="@git.closest.tag.name@" \
org.opencontainers.image.vendor="Red Hat, Inc." \
org.opencontainers.image.licenses="MIT" \
org.opencontainers.image.title="Schema Pusher" \
org.opencontainers.image.description="Use for pushing schemas to Red Hat's Service Registry"
