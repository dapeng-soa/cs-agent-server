# cs-agent-server

[dapeng-config-server](https://github.com/dapeng-soa/dapeng-config-server) 的socket服务端,用于中转web端事件至agent执行端

## 打包
```sbtshell
sbt clean service/assembly
```
- 获得`agent_server_service-assembly-{version}.jar`
- 名字太长了干脆重命名为`agent_server.jar`吧

## 普通启动
将`startup.sh`,`agent_server.jar` 放到一个目录

```sbtshell
agent_erver
  |-startup.sh #启动脚本
  |-agent_server.jar #核心启动程序
```
## Docker容器启动
构建镜像
```
sh buildService.sh
```
> 参考模版配置`compose/agentServer.yml`
启动
```bash
cd compose
docker-compose -f agentServer.yml up -d
```
可选的环境变量：
```sbtshell
socket_server_host=xxx #指定服务启动ip地址,一般使用宿主机ip地址
socket_server_port=xxx #指定服务启动端口
```
