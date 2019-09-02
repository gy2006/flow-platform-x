#!/usr/bin/env bash

version=$1

if [[ -n ${version} ]]; then
  VersionTag="-t flowci/core:$version"
fi

mvn clean package -Dmaven.test.skip=true

docker build -f ./core/Dockerfile -t flowci/core:latest $VersionTag ./core