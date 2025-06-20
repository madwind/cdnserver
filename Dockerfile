ARG JAVA_VERSION
FROM openjdk:${JAVA_VERSION}-jdk-slim
MAINTAINER madwind.cn@gmail.com
COPY build/libs/downloadproxy.jar /
ENTRYPOINT exec java -jar /downloadproxy.jar
