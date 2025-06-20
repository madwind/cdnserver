ARG JAVA_VERSION
ARG VERSION
FROM openjdk:${JAVA_VERSION}-jdk-slim
MAINTAINER madwind.cn@gmail.com
COPY build/libs/downloadproxy-${VERSION}.jar /downloadproxy.jar
ENTRYPOINT exec java -jar /downloadproxy.jar
