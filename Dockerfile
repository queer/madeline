FROM maven:3.5.3-jdk-8

COPY . /app
WORKDIR /app
RUN mvn -B -q clean package

FROM openjdk:8-jre-alpine
COPY --from=0 /app/target/madeline*.jar /app/madeline.jar

WORKDIR /app

ENTRYPOINT ["/usr/bin/java", "-Xmx128M","-jar", "/app/madeline.jar"]