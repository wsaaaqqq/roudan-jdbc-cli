FROM eclipse-temurin:8-jre-alpine AS builder
WORKDIR /app
COPY target/roudan-jdbc-cli.jar /app/lib/roudan-jdbc-cli.jar
RUN echo '#!/bin/sh' > /app/roudan-jdbc-cli && \
    echo 'exec java -jar /app/lib/roudan-jdbc-cli.jar "$@"' >> /app/roudan-jdbc-cli && \
    chmod +x /app/roudan-jdbc-cli

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=builder /app /app
ENV PATH="/app:${PATH}"
LABEL org.opencontainers.image.title="roudan-jdbc-cli"
LABEL org.opencontainers.image.description="JDBC CLI tool for AI agents"
LABEL org.opencontainers.image.source="https://github.com/wsaaaqqq/roudan-jdbc-cli"
ENTRYPOINT ["roudan-jdbc-cli"]
CMD ["--help"]
