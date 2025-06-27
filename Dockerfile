ARG JAVA_VERSION

FROM openjdk:${JAVA_VERSION}-jdk-slim

ARG VERSION

MAINTAINER madwind.cn@gmail.com

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/dlproxy-${VERSION}.jar /dlproxy.jar

ENTRYPOINT exec java -jar /dlproxy.jar
