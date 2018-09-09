From openjdk:8

ENV WORKER=/flow.ci
ENV JAR=flow-ci-core.jar

WORKDIR $WORKER

COPY target/$JAR .

EXPOSE 8080

CMD java $JVM_OPS -jar $JAR