#!/bin/bash

export AGENT_TOKEN=458b7484-e6ce-4af4-a582-ddec163fbaa9
export AGENT_SERVER_URL=http://127.0.0.1:8080

docker-compose -f docker-compose.agent.yml up -d