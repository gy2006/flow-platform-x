#!/bin/bash

###############################
#
#   flow.ci core api shell
#
#   ${1}: your host ip
#   ${2}: up | restart | stop
#
###############################

export HOST_IP=${1}
OPERATION_CMD=${2}
BASE_CMD="docker-compose -f docker-compose.core.yml"

echo "Host ip is ${HOST_IP}"

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

