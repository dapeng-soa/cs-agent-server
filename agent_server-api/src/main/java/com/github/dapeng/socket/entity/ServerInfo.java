package com.github.dapeng.socket.entity;

import com.google.gson.Gson;

/**
 * webAgent serverTime 事件请求格式:
 * List<String> serviceNames</>
 *
 * agent getServerTimeResp事件返回格式: (time的时间到秒)
 * socketId:serviceName:ip:time:status
 *
 * server处理方式
 * 将agent返回构造serverTimeInfo
 * @author struy
 */
public class ServerInfo {

    private String socketId;
    private String ip;
    private String nameSpace;
    private String serviceName;
    private long time;
    private boolean status;
    private String tag;
    private String replicasStatus;


    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getReplicasStatus() {
        return replicasStatus;
    }

    public void setReplicasStatus(String replicasStatus) {
        this.replicasStatus = replicasStatus;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


}

