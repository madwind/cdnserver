ARG JAVA_VERSION
FROM openjdk:${JAVA_VERSION}-jdk-slim
ARG VERSION
MAINTAINER madwind.cn@gmail.com
COPY build/libs/dlproxy-${VERSION}.jar /dlproxy.jar
ENTRYPOINT exec java -jar /dlproxy.jar
