## yacy_grid_mcp dockerfile
## examples:
# docker build -t yacy_grid_mcp .
# docker run -d --rm -p 8100:8100 --name yacy_grid_mcp yacy_grid_mcp
## Check if the service is running:
# curl http://localhost:8100/yacy/grid/mcp/info/status.json

# build app
FROM adoptopenjdk/openjdk8:alpine AS appbuilder
COPY ./ /app
WORKDIR /app
RUN ./gradlew assemble

# build dist
FROM adoptopenjdk/openjdk8:alpine
LABEL maintainer="Michael Peter Christen <mc@yacy.net>"
ENV DEBIAN_FRONTEND noninteractive
ARG default_branch=master
COPY ./conf /app/conf/
COPY --from=appbuilder /app/build/libs/ ./app/build/libs/
WORKDIR /app
EXPOSE 8100
CMD ["java", "-jar", "/app/build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT-all.jar"]
