FROM dapengsoa/openjdk:java8-with-arthas
CMD mkdir -h /agent_server
COPY agent_server.jar /agent_server/
WORKDIR /agent_server
CMD java -jar /agent_server/agent_server.jar
