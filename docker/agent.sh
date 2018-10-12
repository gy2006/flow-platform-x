#!/bin/bash

###############################
#
#   flow.ci agent shell
#
#   ${1}: flow.ci server url, ex: http://127.0.0.1:8080
#   ${2}: your agent token created from flow.ci
#   ${3}: up | restart | stop
#
###############################

export AGENT_SERVER_URL=${1}
export AGENT_TOKEN=${2}
OPERATION_CMD=${3}

BASE_CMD="docker-compose -f docker-compose.agent.yml"

if [[ $OPERATION_CMD = 'up' ]]; then
  ${BASE_CMD} up -d
fi

if [[ $OPERATION_CMD = 'stop' ]]; then
  ${BASE_CMD} stop
fi

if [[ $OPERATION_CMD = 'restart' ]]; then
  ${BASE_CMD} stop
  ${BASE_CMD} rm -f
  ${BASE_CMD} pull
  ${BASE_CMD} up -d
fi