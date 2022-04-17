FROM openjdk:17
MAINTAINER madwind.cn@gmail.com
COPY cdnserver.jar /
ENTRYPOINT "java -jar /cdnserver.jar"
