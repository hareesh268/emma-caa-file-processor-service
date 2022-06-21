FROM docker.repo1.uhc.com/adoptopenjdk/openjdk11:latest
RUN apt update && apt upgrade -y

# Copy the jar file created in build stage into the container
ARG JAR_FILE=target/emma-caa-file-processor-service.jar

COPY ${JAR_FILE} app.jar
COPY certs/ /certs/

# Expose port for access to microservice
EXPOSE 8080

# Add a new user and group to the system so container does not run as root
RUN addgroup -gid 2000 -System emma && adduser -System -u 2000 -Group emma

#RUN addgroup -g 2000 -S emma && adduser -S -u 2000 -G emma emma

RUN chown -R emma:emma /opt
RUN mkdir /logs && chown -R emma:emma /logs

USER 2000

#Contrast Scan changes
COPY contrast/ /home/emma/
COPY enable_and_switch_contrast_environment.sh /home/emma/
ARG WITH_CONTRAST=0
#RUN chmod 755 ./enable_and_switch_contrast_environment.sh

# Run the microservice
#ENTRYPOINT ["java","-Dserver.max-http-header-size=65536","-jar","/app.jar"]
#ENTRYPOINT ["./enable_and_switch_contrast_environment.sh"]
CMD java -javaagent:/home/emma/contrast-agent.jar -Dcontrast.config.path=/home/emma/dev/contrast_security.yaml -Dcontrast.log=/home/emma/ -Dserver.max-http-header-size=65536 -jar /app.jar