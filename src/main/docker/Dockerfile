FROM openjdk:17-jre-alpine
WORKDIR /web
ADD target/*.jar /web/application.jar
ENTRYPOINT exec java -jar "/web/application.jar"
EXPOSE 6000