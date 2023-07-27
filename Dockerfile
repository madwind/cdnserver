FROM openjdk:17.0.2-jdk-slim
MAINTAINER madwind.cn@gmail.com
COPY build/libs/cdnserver.jar /
ENTRYPOINT exec java -jar /cdnserver.jar
