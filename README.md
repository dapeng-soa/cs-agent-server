# cs-agent-server

[dapeng-config-server](https://github.com/dapeng-soa/dapeng-config-server) 的socket服务端,用于中转web端事件至agent执行端

[cs-agent-client](https://github.com/dapeng-soa/cs-agent-client) 执行客户端参考

## Jar启动,下载程序包
点击进入下载: [agent_server_2.2.1.tar.gz](https://github.com/dapeng-soa/cs-agent-server/releases/tag/2.2.1)

解压后目录结构如下:
```shell
agent_erver
  |-startup.sh #启动脚本
  |-agent_server.jar #核心启动程序
```

脚本启动:
```
sh startup.sh
```

## 使用Docker容器启动
> agentServer.yml

```
version: '2'
services:
  agentServer:
    container_name: agentServer
    image: dapengsoa/cs-agent-server:2.2.1
    environment:
      socket_server_port: 6886
      build_enable: true
      DB_CONFIG_SERVER_URL: jdbc:mysql://127.0.0.1:3306/config_server_db?useUnicode=true&zeroDateTimeBehavior=convertToNull
      DB_CONFIG_SERVER_USER: root
      DB_CONFIG_SERVER_PASSWD: 123456
    ports:
      - 6886:6886
    volumes:
      - ~/data/logs/agent_server:/agent_server/logs
```

启动
```bash
docker-compose -f agentServer.yml up -d
```
可选的环境变量：
```sbtshell
DB_CONFIG_SERVER_URL=xxx
DB_CONFIG_SERVER_USER=xxx
DB_CONFIG_SERVER_PASSWD=xxx
socket_server_host=xxx #指定服务启动ip地址,一般使用宿主机ip地址
socket_server_port=xxx #指定服务启动端口
```
