# Stage 1: Install OpenJDK
FROM eclipse-temurin:21-alpine As openjdk

# Stage 2: Install Google Cloud-SDK as the base image
FROM google/cloud-sdk:alpine

# Copy OpenJDK 19 files from the previous stage
COPY --from=openjdk /opt/java/openjdk /opt/java/openjdk

# Install client application
WORKDIR /opt/bench/

COPY run.sh /opt/bench/

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$PATH:$JAVA_HOME/bin"
ENV PROJECT_ID=default
ENV BUCKET=default
ENV APP=default
ENV PROTOCOL=H1
ENV BACKEND_HOST=127.0.0.1
ENV BACKEND_PORT=8090

EXPOSE 8080

CMD ["sh", "-c", "/opt/bench/run.sh ${PROJECT_ID} ${BUCKET} ${APP} ${PROTOCOL} ${BACKEND_HOST} ${BACKEND_PORT}"]