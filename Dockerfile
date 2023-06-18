FROM openjdk:19
COPY out/artifacts/Kasper_jar/Kasper.jar /out/artifacts/Kasper_jar/
WORKDIR /out/artifacts/Kasper_jar/
RUN chmod +r Kasper.jar
CMD java -jar Kasper.jar
