FROM eclipse-temurin:17
RUN mkdir /app
COPY TMPS.jar /app/
ENTRYPOINT ["java", "-jar", "/app/TMPS.jar"]