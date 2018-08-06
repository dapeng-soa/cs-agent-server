FROM docker.today36524.com.cn:5000/base/openjdk:server-jre8
CMD mkdir -h /agent_server && mkdir -h /agent_server/logs
COPY agent_server.jar /agent_server/
WORKDIR /agent_server
CMD java -jar agent_server.jar >> /agent_server/logs/agent_server.log 2>&1 &