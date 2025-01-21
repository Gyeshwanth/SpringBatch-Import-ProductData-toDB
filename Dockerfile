FROM openjdk:17

LABEL maintainer="yeshwanth"

COPY target/SpringBatch-Product-Import.jar /usr/app/SpringBatch-Product-Import.jar

WORKDIR /usr/app

EXPOSE 2001

ENTRYPOINT ["java", "-jar", "SpringBatch-Product-Import.jar"]


