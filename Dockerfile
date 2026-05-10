# Stage 1: Building
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Running app
FROM tomcat:10.1-jdk17
WORKDIR /usr/local/tomcat
RUN rm -rf webapps/*
COPY --from=build /app/target/adboard-1.0.0.war webapps/adboard.war
EXPOSE 8080
CMD ["catalina.sh", "run"]