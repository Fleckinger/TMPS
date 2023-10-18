FROM eclipse-temurin
RUN mkdir /app
COPY TMPS-0.0.1.jar /app/TMPS-0.0.1.jar
WORKDIR /app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]