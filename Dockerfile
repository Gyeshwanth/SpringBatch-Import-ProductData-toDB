//use java 17 as base image then author yeshwanth and copy the jar file to the image
FROM openjdk:17
LABEL maintainer="yeshwanth"

COPY target/SpringBatch-Product-Import.jar /usr/app/SpringBatch-Product-Import.jar

WORKDIR /usr/app

EXPOSE 2001

ENTRYPOINT ["java", "-jar", "SpringBatch-Product-Import.jar"]


