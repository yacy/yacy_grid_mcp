## yacy_grid_mcp Dockerfile
#
# Build:
# docker build -t yacy_grid_mcp .
#
# Run:
# docker run -d -p 127.0.0.1:8100:8100 --link yacy_grid_minio --link yacy_grid_rabbitmq --link yacy_grid_elasticsearch -e YACYGRID_GRID_S3_ADDRESS=admin:12345678@yacy_grid_minio:9000 -e YACYGRID_GRID_BROKER_ADDRESS=guest:guest@yacy_grid_rabbitmq:5672 -e YACYGRID_GRID_ELASTICSEARCH_ADDRESS=yacy_grid_elasticsearch:9300 --name yacy_grid_mcp yacy_grid_mcp
#
# Check if the service is running:
# curl http://localhost:8100/yacy/grid/mcp/info/status.json

## build app
FROM eclipse-temurin:8-jdk-alpine AS appbuilder
COPY ./ /app
WORKDIR /app
RUN ./gradlew assemble

## build dist
FROM eclipse-temurin:8-jre-alpine
LABEL maintainer="Michael Peter Christen <mc@yacy.net>"
ENV DEBIAN_FRONTEND noninteractive
ARG default_branch=master
COPY ./conf /app/conf/
COPY --from=appbuilder /app/build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT-all.jar ./app/build/libs/
WORKDIR /app
EXPOSE 8100
CMD ["java", "-jar", "/app/build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT-all.jar"]
