FROM dhub.kubesre.xyz/eclipse-temurin:17
WORKDIR /web
ADD *.jar /web/application.jar
ENV SPRING_CONFIG_LOCATION=/web/src/main/resources/,classpath:/
ENTRYPOINT exec java -jar "/web/application.jar"
EXPOSE 6000