From openjdk:8

ENV WORKER=/flow.ci
ENV JAR=flow-ci-core.jar

WORKDIR $WORKER

COPY target/$JAR .
CMD java $JVM_OPS -jar $JAR