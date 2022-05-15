## yacy_grid_mcp Dockerfile
#
# Build:
# docker build -t yacy-grid-mcp .
#
# Run:
# docker run -d -p 127.0.0.1:8100:8100 --link yacy-grid-minio --link yacy-grid-rabbitmq --link yacy-grid-elasticsearch -e YACYGRID_GRID_S3_ADDRESS=admin:12345678@yacy-grid-minio:9000 -e YACYGRID_GRID_BROKER_ADDRESS=guest:guest@yacy-grid-rabbitmq:5672 -e YACYGRID_GRID_ELASTICSEARCH_ADDRESS=yacy-grid-elasticsearch:9300 --name yacy-grid-mcp yacy-grid-mcp
#
# Check if the service is running:
# curl http://localhost:8100/yacy/grid/mcp/info/status.json

## build app
FROM adoptopenjdk/openjdk8:alpine AS appbuilder
COPY ./ /app
WORKDIR /app
RUN ./gradlew assemble

## build dist
FROM adoptopenjdk/openjdk8:alpine
LABEL maintainer="Michael Peter Christen <mc@yacy.net>"
ENV DEBIAN_FRONTEND noninteractive
ARG default_branch=master
COPY ./conf /app/conf/
COPY --from=appbuilder /app/build/libs/ ./app/build/libs/
WORKDIR /app
EXPOSE 8100
CMD ["java", "-jar", "/app/build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT-all.jar"]
