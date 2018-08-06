#!/usr/bin/env bash
pwd=`pwd`
buildPackage="/agent_server-service/target/scala-2.12/agent_server_service-assembly-0.1-SNAPSHOT.jar"
echo build on ${pwd}
sbt clean service/assembly
#cp ${pwd}${buildPackage} docker/agent_server.jar
#cd docker
#docker build -t docker.today36524.com.cn:5000/basic/agent-server:1.0.0 .
#rm agent_server.jar
#cd ${pwd}