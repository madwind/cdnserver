FROM openjdk:17
MAINTAINER madwind.cn@gmail.com
COPY build/libs/cdnserver.jar /
ENTRYPOINT "java -jar /cdnserver.jar"
