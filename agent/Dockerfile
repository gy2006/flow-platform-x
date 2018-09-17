From flowci/agent-base

ENV WORKER=/flow.ci.agent
ENV JAR=flow-ci-agent.jar

WORKDIR $WORKER

COPY target/$JAR .
CMD java $JVM_OPS -jar $JAR