FROM maven:3.9-eclipse-temurin-17 AS compilacion

WORKDIR /src

COPY pom.xml ./
COPY servicio-logistica/pom.xml servicio-logistica/pom.xml
COPY servicio-donaciones/pom.xml servicio-donaciones/pom.xml
COPY servicio-logistica/src/main servicio-logistica/src/main
COPY servicio-donaciones/src/main servicio-donaciones/src/main

RUN mvn -B -ntp -Dmaven.test.skip=true package \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies \
    -DincludeScope=runtime -DoutputDirectory=target/lib


FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 donatrack \
    && useradd --uid 10001 --gid 10001 --create-home donatrack \
    && mkdir -p /app/datos/recepciones \
    && chown -R 10001:10001 /app

WORKDIR /app

USER 10001:10001

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -Duser.timezone=America/Argentina/Buenos_Aires"

ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*"]


FROM runtime AS donaciones

COPY --from=compilacion /src/servicio-donaciones/target/servicio-donaciones-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=compilacion /src/servicio-donaciones/target/lib/ /app/lib/

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8083/health || exit 1

CMD ["org.example.server.App"]


FROM runtime AS logistica

COPY --from=compilacion /src/servicio-logistica/target/servicio-logistica-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=compilacion /src/servicio-logistica/target/lib/ /app/lib/

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8082/health || exit 1

CMD ["org.example.LogisticaApplication"]


FROM logistica AS puente

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8084/health || exit 1

CMD ["org.example.planificacion.PuenteHeiGit"]