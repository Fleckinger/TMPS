FROM eclipse-temurin
RUN mkdir /app
COPY TMPS.jar /app/TMPS.jar
WORKDIR /app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "TMPS.jar"]