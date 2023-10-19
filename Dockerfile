FROM eclipse-temurin:17
RUN mkdir /app
COPY TMPS.jar /app/TMPS.jar
ENTRYPOINT ["java", "-jar", "/app/TMPS.jar"]