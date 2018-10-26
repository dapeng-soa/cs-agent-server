package com.github.dapeng.socket.entity;

public class ServiceBuildResponse {

    private long id;
    private String agentHost;
    private String buildService;
    private long taskId;
    private int status;
    private StringBuilder content;

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public StringBuilder getContent() {
        return content;
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }
}
