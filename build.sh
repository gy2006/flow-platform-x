#!/usr/bin/env bash

mvn clean package -Dmaven.test.skip=true

docker build -f ./core/Dockerfile -t flowci/core:latest ./core