package com.github.dapeng.socket.entity;

import java.util.List;

public class BuildVo {


    private long id;
    private String agentHost;
    private String buildService;
    private long taskId;

//    private int status;
//    private String content;


    private List<DependServiceVo> buildServices;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public void setAgentHost(String agentHost) {
        this.agentHost = agentHost;
    }

    public String getBuildService() {
        return buildService;
    }

    public void setBuildService(String buildService) {
        this.buildService = buildService;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public List<DependServiceVo> getBuildServices() {
        return buildServices;
    }

    public void setBuildServices(List<DependServiceVo> buildServices) {
        this.buildServices = buildServices;
    }
}
