package com.github.dapeng.socket.entity;

/**
 * @author with struy.
 * Create by 2019-01-13 22:00
 * email :yq1724555319@gmail.com
 */

public class CmdRequest {
    private String sourceClientId;
    private String ip;
    private String containerId;
    private String data;

    public String getSourceClientId() {
        return sourceClientId;
    }

    public void setSourceClientId(String sourceClientId) {
        this.sourceClientId = sourceClientId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
