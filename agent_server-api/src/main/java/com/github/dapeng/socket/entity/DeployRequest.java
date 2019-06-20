package com.github.dapeng.socket.entity;

import java.util.Objects;

public class DeployRequest {

    private String ip;
    //host name
    private String namespace;
    private String serviceName;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeployRequest request = (DeployRequest) o;
        return Objects.equals(ip, request.ip) &&
                Objects.equals(serviceName, request.serviceName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ip, serviceName);
    }

    @Override
    public String toString() {
        return "DeployRequest{" +
                "ip='" + ip + '\'' +
                ", namespace='" + namespace + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
