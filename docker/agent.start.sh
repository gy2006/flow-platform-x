#!/bin/bash

export AGENT_SERVER_URL=${1}
export AGENT_TOKEN=${2}

docker-compose -f docker-compose.agent.yml up -d