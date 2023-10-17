FROM eclipse-temurin
VOLUME /tmp
COPY  build/libs/TMPS-0.0.1-SNAPSHOT.jar TMPS-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/TMPS-0.0.1-SNAPSHOT.jar"]