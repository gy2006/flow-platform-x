#!/bin/bash

mvn clean package -Dmaven.test.skip=true

docker build -f ./Dockerfile -t flowci/core:latest .