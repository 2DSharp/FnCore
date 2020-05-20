#
# Build stage
#
FROM maven:3.6.0-jdk-8-slim AS build
RUN echo 'Copying files...'
COPY src /home/app/src
COPY pom.xml /home/app
RUN echo 'Downloading dependencies...'
RUN mvn -B -f /home/app/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:go-offline
RUN echo 'Building...'
RUN mvn -f /home/app/pom.xml package -P docker

#
# Package stage
#
FROM openjdk:8-jre-slim
COPY --from=build /home/app/target/friendlyneighbor-core.jar /home/app/fncore.jar
EXPOSE 9120
ENTRYPOINT ["java","-jar","/home/app/fncore.jar"]