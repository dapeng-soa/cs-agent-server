package com.github.dapeng.socket.server;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.github.dapeng.socket.AgentEvent;
import com.github.dapeng.socket.HostAgent;
import com.github.dapeng.socket.entity.DeployRequest;
import com.github.dapeng.socket.entity.DeployVo;
import com.github.dapeng.socket.entity.ServerTimeInfo;
import com.github.dapeng.socket.enums.EventType;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        init("127.0.0.1", 9095);
    }

    public static void init(String hostName, int port) {
        Configuration config = new Configuration();
        config.setPort(port);
        config.setHostname(hostName);

        config.setAllowCustomRequests(true);

        Map<String, HostAgent> nodesMap = new ConcurrentHashMap<String, HostAgent>();
        Map<String, List<ServerTimeInfo>> serverDeployTime = new ConcurrentHashMap<>();
        Map<String, HostAgent> webClientMap = new ConcurrentHashMap<String, HostAgent>();

        final SocketIOServer server = new SocketIOServer(config);
        final BlockingQueue queue = new LinkedBlockingQueue();


        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                System.out.println(String.format(socketIOClient.getRemoteAddress() + " --> join room %s", socketIOClient.getSessionId()));
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                if (nodesMap.containsKey(socketIOClient.getSessionId().toString())) {
                    socketIOClient.leaveRoom("nodes");
                    nodesMap.remove(socketIOClient.getSessionId().toString());

                    System.out.println(String.format("leave room  nodes %s", socketIOClient.getSessionId()));
//                    notifyWebClients(nodesMap, server);
                }

                if (webClientMap.containsKey(socketIOClient.getSessionId().toString())) {
                    socketIOClient.leaveRoom("web");
                    System.out.println(String.format("leave room web  %s", socketIOClient.getSessionId()));
                }
            }
        });

        server.addEventListener("nodeReg", String.class, new DataListener<String>() {
                    @Override
                    public void onData(SocketIOClient client,
                                       String data, AckRequest ackRequest) {
                        client.joinRoom("nodes");
                        System.out.println("nodes Reg");
                        String name = data.split(":")[0];
                        String ip = data.split(":")[1];
                        nodesMap.put(client.getSessionId().toString(), new HostAgent(name, ip, client.getSessionId().toString()));
//                        notifyWebClients(nodesMap, server);
                    }
                }

        );


        server.addEventListener("webReg", String.class, new DataListener<String>() {
                    @Override
                    public void onData(SocketIOClient client,
                                       String data, AckRequest ackRequest) {
                        client.joinRoom("web");
                        System.out.println("web Reg..." + client.getSessionId());
                        String name = data.split(":")[0];
                        String ip = data.split(":")[1];
                        webClientMap.put(client.getSessionId().toString(), new HostAgent(name, ip, client.getSessionId().toString()));
//                        notifyWebClients(nodesMap, server);
                    }
                }

        );

        server.addEventListener(EventType.WEB_EVENT().name(), String.class, new DataListener<String>() {

            @Override
            public void onData(SocketIOClient socketIOClient, String agentEvent, AckRequest ackRequest) throws Exception {
                // logger.info(" receive webEvent: " + agentEvent.getCmd());
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
            }
        });

        //发送指令给agent获取当前节点的部署时间
        server.addEventListener(EventType.GET_SERVER_TIME().name(), String.class, new DataListener<String>() {
                    @Override
                    public void onData(SocketIOClient client,
                                       String data, AckRequest ackRequest) {
                        System.out.println(" received serverTime cmd....." + data);
                        serverDeployTime.clear();
                        System.out.println(server.getRoomOperations("nodes").getClients().size());
                        server.getRoomOperations("nodes").sendEvent(EventType.GET_SERVER_TIME().name(),data);
                    }
                }
        );

        //获取到agent返回的时间，并转发给web节点
        server.addEventListener(EventType.GET_SERVER_TIME_RESP().name(), String.class, new DataListener<String>() {
                    @Override
                    public void onData(SocketIOClient client,
                                       String data, AckRequest ackRequest) {
                        String[] tempData = data.split(":");
                        String socketId = tempData[0];
                        String serviceName = tempData[1];
                        String ip = tempData[2];
                        String time = tempData[3];
                        System.out.println(" received getServerTimeResp cmd..." + data);
                        ServerTimeInfo info = new ServerTimeInfo();
                        info.setSocketId(socketId);
                        info.setIp(ip);
                        info.setServiceName(serviceName);
                        info.setTime(Long.valueOf(time));

                        if (serverDeployTime.containsKey(serviceName)) {
                            serverDeployTime.get(serviceName).add(info);
                        } else {
                            List<ServerTimeInfo> infos = new ArrayList<>();
                            infos.add(info);
                            serverDeployTime.put(serviceName, infos);
                        }


                        List<String> sentServices = new ArrayList<>();
                        serverDeployTime.values().forEach(i -> {
                            if (i.size() == nodesMap.size()) {
                                System.out.println(server.getRoomOperations("web").getClients().size());
                                server.getRoomOperations("web").sendEvent(EventType.GET_SERVER_TIME_RESP().name(), new Gson().toJson(i));
                                sentServices.add(serviceName);
                            }
                        });

                        sentServices.forEach(serverDeployTime::remove);

                    }
                }
        );

        server.addEventListener(EventType.GET_YAML_FILE().name(), String.class, (client,
                data, ackRequest) -> {
            System.out.println(" server received getYamlFile cmd");

        });

        server.addEventListener(EventType.DEPLOY().name(), String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client,
                               String data, AckRequest ackRequest) {
                DeployVo vo = new Gson().fromJson(data, DeployVo.class);

                nodesMap.values().forEach(agent -> {
                    if (vo.getIp().equals(agent.getIp())) {
                        SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                        if (targetAgent != null) {
                            targetAgent.sendEvent(EventType.DEPLOY().name(), data);
                        }
                    }
                });
                server.getRoomOperations("nodes").sendEvent(EventType.DEPLOY().name(), data);
            }
        });


        server.addEventListener(EventType.STOP().name(), String.class, (client, data, ackRequest) -> {
            DeployRequest request = new Gson().fromJson(data, DeployRequest.class);

            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.STOP().name(), data);
                    }
                }
            });

            server.getRoomOperations("nodes").sendEvent(EventType.STOP().name(), data);
        });

        server.addEventListener(EventType.RESTART().name(), String.class, (client, data, ackRequest) -> {
            DeployRequest request = new Gson().fromJson(data, DeployRequest.class);

            nodesMap.values().forEach(agent -> {
                if (request.getIp().equals(agent.getIp())) {
                    SocketIOClient targetAgent = server.getClient(UUID.fromString(agent.getSessionId()));
                    if (targetAgent != null) {
                        targetAgent.sendEvent(EventType.RESTART().name(), data);
                    }
                }
            });

            server.getRoomOperations("nodes").sendEvent(EventType.STOP().name(), data);
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


//    private static void notifyWebClients(Map<String, HostAgent> map, SocketIOServer server) {
//        Collection<HostAgent> agents = map.values();
//
//        System.out.println(" current agent clients size: " + agents.stream().map(i -> i.getIp()).collect(Collectors.toList()));
//
//        server.getRoomOperations("nodes").sendEvent("serverList", agents);
//
//    }
}
