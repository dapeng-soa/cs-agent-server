FROM docker.today36524.com.cn:5000/base/openjdk:server-jre8
CMD mkdir -h /agent_server && mkdir -h /agent_server/logs
COPY agent_server.jar /agent_server/
ENV SERVER_HOST=""
ENV SERVER_PORT=""
WORKDIR /agent_server
ENTRYPOINT ["sh","-c","java -jar agent_server.jar $SERVER_HOST $SERVER_PORT >> /agent_server/logs/agent_server.log 2>&1 &"]