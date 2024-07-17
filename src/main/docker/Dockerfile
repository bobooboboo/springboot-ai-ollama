FROM dhub.kubesre.xyz/eclipse-temurin:17
WORKDIR /web
ADD *.jar /web/application.jar
ENTRYPOINT exec java -jar "/web/application.jar"
EXPOSE 6000