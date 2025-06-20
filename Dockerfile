ARG JAVA_VERSION
FROM openjdk:${JAVA_VERSION}-jdk-slim
ARG VERSION
MAINTAINER madwind.cn@gmail.com
COPY build/libs/downloadproxy-${VERSION}.jar /downloadproxy.jar
ENTRYPOINT exec java -jar /downloadproxy.jar
