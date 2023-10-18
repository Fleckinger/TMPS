FROM eclipse-temurin
RUN mkdir /app
COPY TMPS.jar /app/TMPS.jar
WORKDIR /app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]