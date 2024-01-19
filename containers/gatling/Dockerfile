# Stage 1: Install OpenJDK 19
FROM eclipse-temurin:21 As openjdk

# Stage 2: Install Google Cloud-SDK as the base image
FROM google/cloud-sdk:slim

# Copy OpenJDK 19 files from the previous stage
COPY --from=openjdk /opt/java/openjdk /opt/java/openjdk

# Install "jq" tool (for building json files from bash)
RUN apt-get install -y unzip jq procps

# Install client application
WORKDIR /tmp/bench/

COPY gatling-*-all.jar /tmp/bench/
COPY run.sh /tmp/bench/

# Replace with inline env variables when running the container
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$PATH:$JAVA_HOME/bin"
ENV SERVER_HOST=localhost
ENV SERVER_PORT=8080
ENV SIMULATIONS="TextPlain"
ENV BUCKET=default
ENV DURATION=20
ENV APP_NAME=default
ENV PROTOCOL=H2
ENV STEPS=10
ENV INCREMENT=100

CMD ["sh", "-c", "/tmp/bench/run.sh ${SERVER_HOST} ${SERVER_PORT} ${SIMULATIONS} ${DURATION} ${APP_NAME} ${PROTOCOL} ${STEPS} ${INCREMENT}"]