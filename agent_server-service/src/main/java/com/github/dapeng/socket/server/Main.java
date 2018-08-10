package com.github.dapeng.socket.server;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.github.dapeng.socket.AgentEvent;
import com.github.dapeng.socket.HostAgent;
import com.github.dapeng.socket.entity.DeployRequest;
import com.github.dapeng.socket.entity.DeployVo;
import com.github.dapeng.socket.entity.ServerInfo;
import com.github.dapeng.socket.enums.EventType;
import com.github.dapeng.socket.util.IPUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private static List<DeployRequest> services = new ArrayList<>(32);

    private static boolean timed = false;

    public static void main(String[] args) {
        String host = IPUtils.localIp();
        int port = 6886;
        if (args != null && args.length >= 2) {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }
        System.out.println("===> socket server initing :" + host + ":" + port);
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


        server.addConnectListener(socketIOClient -> System.out.println(String.format(socketIOClient.getRemoteAddress() + " --> join room %s", socketIOClient.getSessionId())));

        server.addDisconnectListener(socketIOClient -> {
            if (nodesMap.containsKey(socketIOClient.getSessionId().toString())) {
                socketIOClient.leaveRoom("nodes");
                nodesMap.remove(socketIOClient.getSessionId().toString());

                System.out.println(String.format("leave room  nodes %s", socketIOClient.getSessionId()));
            }

            if (webClientMap.containsKey(socketIOClient.getSessionId().toString())) {
                socketIOClient.leaveRoom("web");
                System.out.println(String.format("leave room web  %s", socketIOClient.getSessionId()));
                webClientMap.remove(socketIOClient.getSessionId().toString());
                //timer.shutdown();
                //timed = !timer.isShutdown();
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
                    System.out.println("nodes Reg");
                    String name = data.split(":")[0];
                    String ip = data.split(":")[1];
                    nodesMap.put(client.getSessionId().toString(), new HostAgent(name, ip, client.getSessionId().toString()));
                }

        );


        server.addEventListener(EventType.WEB_REG().name(), String.class, (client, data, ackRequest) -> {
                    client.joinRoom("web");
                    System.out.println("web Reg..." + client.getSessionId());
                    String name = data.split(":")[0];
                    String ip = data.split(":")[1];
                    webClientMap.put(client.getSessionId().toString(), new HostAgent(name, ip, client.getSessionId().toString()));
                }

        );

        server.addEventListener(EventType.WEB_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            System.out.println("==================================================");
            System.out.println(" agentEvent: " + agentEvent);

            AgentEvent agentEventObj = new Gson().fromJson(agentEvent, AgentEvent.class);
            System.out.println(" agentEventObj: " + agentEventObj);

            agentEventObj.getClientSessionIds().forEach(sessionId -> {
                SocketIOClient client = server.getClient(UUID.fromString(sessionId));
                if (client != null) {
                    client.sendEvent(EventType.WEB_EVENT().name(), agentEvent);
                } else {
                    System.out.println(" Failed to get socketClient......");
                }
            });
        });


        server.addEventListener(EventType.NODE_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            System.out.println("==================================================");
            System.out.println(" agentEvent: " + agentEvent);

            server.getRoomOperations("web").sendEvent(EventType.NODE_EVENT().name(), agentEvent);
        });

        server.addEventListener(EventType.ERROR_EVENT().name(), String.class, (socketIOClient, agentEvent, ackRequest) -> {
            System.out.println(" errorEvent: " + agentEvent);

            server.getRoomOperations("web").sendEvent(EventType.ERROR_EVENT().name(), agentEvent);
        });

        //发送指令给agent获取当前节点的部署时间
        server.addEventListener(EventType.GET_SERVER_INFO().name(), String.class, (client, data, ackRequest) -> {
                    System.out.println("server received serverTime cmd....." + data);
                    List<DeployRequest> requests = new Gson().fromJson(data, new TypeToken<List<DeployRequest>>() {
                    }.getType());

                    // 如有修改应当拷贝一份,定时器需要更新查询的数据
                    services = requests;

                    // 定时发送所有的服务状态检查，但需要做状态判断，只能启动一次定时器
                    if (!timed) {
                        timer.scheduleAtFixedRate(() -> {
                            System.out.println(":::timing send getServiceInfo runing");
                            sendGetServiceInfo(nodesMap, server);
                        }, 0, 10000, TimeUnit.MILLISECONDS);
                        timed = true;
                    } else {
                        System.out.println(":::warn getServiceInfo  is Timing ,skip");
                    }
                    // 当再次发起调用需要即时发送检查
                    sendGetServiceInfo(nodesMap, server);
                }
        );

        //获取到agent返回的时间，并转发给web节点
        server.addEventListener(EventType.GET_SERVER_INFO_RESP().name(), String.class, (client, data, ackRequest) -> {
                    System.out.println(" received getServerTimeResp cmd..." + data);
                    String[] tempData = data.split(":");
                    String socketId = tempData[0];
                    String ip = tempData[1];
                    String serviceName = tempData[2];
                    boolean status = Boolean.valueOf(tempData[3]);
                    String time = tempData[4];
                    ServerInfo info = new ServerInfo();
                    info.setSocketId(socketId);
                    info.setIp(ip);
                    info.setServiceName(serviceName);
                    info.setTime(Long.valueOf(time));
                    info.setStatus(status);
                    // 单个返回
                    server.getRoomOperations("web").sendEvent(EventType.GET_SERVER_INFO_RESP().name(), new Gson().toJson(info));
                }
        );

        server.addEventListener(EventType.GET_YAML_FILE().name(), String.class, (client,
                                                                                 data, ackRequest) -> {
            System.out.println(" server received getYamlFile cmd" + data);
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
            System.out.println(" server received getYamlFileResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.GET_YAML_FILE_RESP().name(), data);
        });

        server.addEventListener(EventType.DEPLOY().name(), String.class, (client, data, ackRequest) -> {
            DeployVo vo = new Gson().fromJson(data, DeployVo.class);
            System.out.println(" server received deploy cmd" + data);
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
            System.out.println(" server received stop cmd" + data);
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
            System.out.println(" server received restart cmd" + data);
            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.RESTART().name(), data);
                    }
                }
            });
        });

        server.addEventListener(EventType.DEPLOY_RESP().name(), String.class, (client,
                                                                               data, ackRequest) -> {
            System.out.println(" server received deployResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.DEPLOY_RESP().name(), data);
        });

        server.addEventListener(EventType.STOP_RESP().name(), String.class, (client,
                                                                             data, ackRequest) -> {
            System.out.println(" server received stopResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.STOP_RESP().name(), data);
        });

        server.addEventListener(EventType.RESTART_RESP().name(), String.class, (client,
                                                                                data, ackRequest) -> {
            System.out.println(" server received restartResp cmd" + data);
            server.getRoomOperations("web").sendEvent(EventType.RESTART_RESP().name(), data);
        });

        server.start();
        System.out.println("websocket server started at " + port);

        CmdExecutor ex = new CmdExecutor(queue, server);
        System.out.println("CmdExecutor Thread started");


        new Thread(ex).start();
        try {

            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            System.out.println(" Failed to sleep.." + e.getMessage());
        }

        server.stop();
    }

    private static void sendGetServiceInfo(Map<String, HostAgent> nodesMap, SocketIOServer server) {
        System.out.println("::: request services[" + services.size() + "]" + services);
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
