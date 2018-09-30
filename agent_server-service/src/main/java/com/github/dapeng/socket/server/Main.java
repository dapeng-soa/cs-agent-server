package com.github.dapeng.socket.server;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.github.dapeng.socket.AgentEvent;
import com.github.dapeng.socket.HostAgent;
import com.github.dapeng.socket.entity.*;
import com.github.dapeng.socket.enums.EventType;
import com.github.dapeng.socket.util.IPUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private static List<DeployRequest> services = new ArrayList<>(32);

    private static boolean timed = false;

    public static void main(String[] args) {
        String host = System.getenv("socket_server_host");
        String serverPort = System.getenv("socket_server_port");
        Integer port = Integer.valueOf(null == serverPort ? "0" : serverPort);
        if (args != null && args.length >= 2) {
            host = args[0];
            port = Integer.valueOf(args[1]);
        } else {
            if (null == host || "".equals(host.trim())) {
                host = IPUtils.localIp();
            }
            if (port == 0) {
                port = 6886;
            }
        }
        LOGGER.info("===> socket server initing => " + host + ":" + port);
        init(host, port);
    }


    private static void init(String hostName, int port) {
        Configuration config = new Configuration();
        config.setPort(port);
        config.setHostname(hostName);
        config.setAllowCustomRequests(true);

        Map<String, HostAgent> nodesMap = new ConcurrentHashMap<>();
        Map<String, HostAgent> webClientMap = new ConcurrentHashMap<>();

        final SocketIOServer server = new SocketIOServer(config);
        final BlockingQueue queue = new LinkedBlockingQueue();


        server.addConnectListener(socketIOClient -> LOGGER.info(String.format(socketIOClient.getRemoteAddress() + " --> join room %s", socketIOClient.getSessionId())));

        server.addDisconnectListener(socketIOClient -> {
            if (nodesMap.containsKey(socketIOClient.getSessionId().toString())) {
                socketIOClient.leaveRoom("nodes");
                nodesMap.remove(socketIOClient.getSessionId().toString());

                LOGGER.info(String.format("leave room  nodes %s", socketIOClient.getSessionId()));
            }

            if (webClientMap.containsKey(socketIOClient.getSessionId().toString())) {
                socketIOClient.leaveRoom("web");
                LOGGER.info(String.format("leave room web  %s", socketIOClient.getSessionId()));
                webClientMap.remove(socketIOClient.getSessionId().toString());
                // web 离开通知所有agent客户端

                nodesMap.values().forEach(agent -> {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.WEB_LEAVE().name(), EventType.WEB_LEAVE().name());
                    }
                });
            }
        });

        server.addEventListener(EventType.NODE_REG().name(), String.class, (client, data, ackRequest) -> {
                    client.joinRoom("nodes");
                    LOGGER.info("nodes Reg");
                    String name = data.split(":")[0];
                    String ip = data.split(":")[1];
                    nodesMap.put(client.getSessionId().toString(), new HostAgent(name, ip, client.getSessionId().toString()));
                }

        );


        server.addEventListener(EventType.WEB_REG().name(), String.class, (client, data, ackRequest) -> {
                    client.joinRoom("web");
                    LOGGER.info("web Reg..." + client.getSessionId());
                    String s = client.getRemoteAddress().toString();
                    String remoteIp = s.replaceFirst("/", "").substring(0, s.lastIndexOf(":") - 1);
                    String name = data.split(":")[0];
                    String ip = data.split(":")[1];
                    webClientMap.put(client.getSessionId().toString(), new HostAgent(name, remoteIp, client.getSessionId().toString()));
                }

        );

        server.addEventListener(EventType.WEB_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            LOGGER.info(" agentEvent: " + agentEvent);

            AgentEvent agentEventObj = new Gson().fromJson(agentEvent, AgentEvent.class);
            LOGGER.info(" agentEventObj: " + agentEventObj);

            agentEventObj.getClientSessionIds().forEach(sessionId -> {
                SocketIOClient client = server.getClient(UUID.fromString(sessionId));
                if (client != null) {
                    client.sendEvent(EventType.WEB_EVENT().name(), agentEvent);
                } else {
                    LOGGER.error(" Failed to get socketClient......");
                }
            });
        });


        server.addEventListener(EventType.NODE_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            LOGGER.info(" agentEvent: " + agentEvent);

            server.getRoomOperations("web").sendEvent(EventType.NODE_EVENT().name(), agentEvent);
        });

        server.addEventListener(EventType.ERROR_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            LOGGER.info(" errorEvent: " + agentEvent);

            server.getRoomOperations("web").sendEvent(EventType.ERROR_EVENT().name(), agentEvent);
        });

        //发送指令给agent获取当前节点的部署时间
        server.addEventListener(EventType.GET_SERVER_INFO().name(), String.class, (client, data, ackRequest) -> {
                    LOGGER.info("server received serverInf cmd....." + data);
                    List<DeployRequest> requests = new Gson().fromJson(data, new TypeToken<List<DeployRequest>>() {
                    }.getType());

                    // 如有修改应当拷贝一份,定时器需要更新查询的数据
                    // fixme 将不同客户端发送的服务都问询一遍,需要去重复，拿并集
                    services = requests;

                    // 定时发送所有的服务状态检查，但需要做状态判断，只能启动一次定时器
                    if (!timed) {
                        timer.scheduleAtFixedRate(() -> {
                            LOGGER.info(":::timing send getServiceInfo runing");
                            sendGetServiceInfo(nodesMap, server);
                        }, 0, 10000, TimeUnit.MILLISECONDS);
                        timed = true;
                    } else {
                        LOGGER.info(":::warn getServiceInfo  is Timing ,skip");
                    }
                    // 当再次发起调用需要即时发送检查
                    sendGetServiceInfo(nodesMap, server);
                }
        );

        //获取到agent返回的时间，并转发给web节点
        server.addEventListener(EventType.GET_SERVER_INFO_RESP().name(), String.class, (client, data, ackRequest) -> {
                    LOGGER.info(" received getServerInfoResp cmd..." + data);
                    String[] tempData = data.split(":");
                    String socketId = tempData[0];
                    String ip = tempData[1];
                    String serviceName = tempData[2];
                    boolean status = Boolean.valueOf(tempData[3]);
                    String time = tempData[4];
                    String tag = tempData[5];
                    ServerInfo info = new ServerInfo();
                    info.setSocketId(socketId);
                    info.setIp(ip);
                    info.setServiceName(serviceName);
                    info.setTime(Long.valueOf(time));
                    info.setStatus(status);
                    info.setTag(tag);
                    // 单个返回
                    server.getRoomOperations("web").sendEvent(EventType.GET_SERVER_INFO_RESP().name(), new Gson().toJson(info));
                }
        );

        server.addEventListener(EventType.GET_YAML_FILE().name(), String.class, (client,
                                                                                 data, ackRequest) -> {
            LOGGER.info(" server received getYamlFile cmd" + data);
            DeployRequest request = new Gson().fromJson(data, DeployRequest.class);
            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.GET_YAML_FILE().name(), data);
                    }
                }
            });
        });

        server.addEventListener(EventType.GET_YAML_FILE_RESP().name(), String.class, (client,
                                                                                      data, ackRequest) -> {
            LOGGER.info(" server received getYamlFileResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.GET_YAML_FILE_RESP().name(), data);
        });

        //发布构建任务
        server.addEventListener(EventType.BUILD().name(), String.class, ((client, data, ackSender) -> {
            BuildVo buildVo = new Gson().fromJson(data, BuildVo.class);
            String buildServerIp = buildVo.getBuildServerIp();
            if (buildServerIp == null && buildServerIp.isEmpty()) {
                server.getClient(client.getSessionId()).sendEvent(EventType.ERROR_EVENT().name(), "构建服务器的IP不能为空");
            } else {
                nodesMap.values().forEach(agent -> {
                    if (buildServerIp.equals(agent.getIp())) {
                        SocketIOClient agentClient = server.getClient(UUID.fromString(agent.getSessionId()));
                        if (agentClient == null) {
                            server.getClient(client.getSessionId()).sendEvent(EventType.ERROR_EVENT().name(), "找不到对应clientAgent: " + agent.getIp());
                        } else {
                            agentClient.sendEvent(EventType.BUILD().name(), new Gson().toJson(buildVo.getBuildServices()));
                        }
                    }
                });
            }

        }));

        server.addEventListener(EventType.DEPLOY().name(), String.class, (client, data, ackRequest) -> {
            DeployVo vo = new Gson().fromJson(data, DeployVo.class);
            LOGGER.info(" server received deploy cmd" + data);
            nodesMap.values().forEach(agent -> {
                if (vo.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.DEPLOY().name(), data);
                    }
                }
            });
        });


        server.addEventListener(EventType.STOP().name(), String.class, (client, data, ackRequest) -> {
            DeployRequest request = new Gson().fromJson(data, DeployRequest.class);
            LOGGER.info(" server received stop cmd" + data);
            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.STOP().name(), data);
                    }
                }
            });
        });

        server.addEventListener(EventType.RESTART().name(), String.class, (client, data, ackRequest) -> {
            DeployRequest request = new Gson().fromJson(data, DeployRequest.class);
            LOGGER.info(" server received restart cmd" + data);
            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.RESTART().name(), data);
                    }
                }
            });
        });
        // 获取agents列表 web -> server -> web
        server.addEventListener(EventType.GET_REGED_AGENTS().name(), String.class, ((client, data, ackSender) -> {
            LOGGER.info("server received getRegedAgents cmd" + data);
            String agents = new Gson().toJson(nodesMap);
            server.getRoomOperations("web").sendEvent(EventType.GET_REGED_AGENTS_RESP().name(), agents);
        }));

        server.addEventListener(EventType.DEPLOY_RESP().name(), String.class, (client,
                                                                               data, ackRequest) -> {
            LOGGER.info(" server received deployResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.DEPLOY_RESP().name(), data);
        });

        server.addEventListener(EventType.STOP_RESP().name(), String.class, (client,
                                                                             data, ackRequest) -> {
            LOGGER.info(" server received stopResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.STOP_RESP().name(), data);
        });

        server.addEventListener(EventType.RESTART_RESP().name(), String.class, (client,
                                                                                data, ackRequest) -> {
            LOGGER.info(" server received restartResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.RESTART_RESP().name(), data);
        });

        server.start();
        LOGGER.info("websocket server started at " + port);

        CmdExecutor ex = new CmdExecutor(queue, server);
        LOGGER.info("CmdExecutor Thread started");


        new Thread(ex).start();
        try {

            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            LOGGER.info(" Failed to sleep.." + e.getMessage());
        }

        server.stop();
    }

    private static void sendGetServiceInfo(Map<String, HostAgent> nodesMap, SocketIOServer server) {
        LOGGER.info("::: request services[" + services.size() + "]" + services);
        services.forEach(request -> {
            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.GET_SERVER_INFO().name(), new Gson().toJson(request));
                    }
                }
            });
        });
    }
}
