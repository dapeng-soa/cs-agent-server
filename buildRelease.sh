#!/usr/bin/env bash
version=2.2.1
pwd=`pwd`
buildPackage="/agent_server-service/target/scala-2.12/agent_server_service-assembly-$version.jar"
echo build on ${pwd}
sbt clean service/assembly
cp ${pwd}${buildPackage} docker/agent_server.jar
cp ${pwd}${buildPackage} dist/agent_server.jar

tar -czvpf $pwd/target/agent_server_$version.tar.gz dist

echo "release package at $pwd/target/agent_server_$version.tar.gz"

echo "build docker image start"

cd docker
docker build -t dapengsoa/cs-agent-server:2.2.1 .
