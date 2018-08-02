package com.github.dapeng.socket.entity;

import com.google.gson.Gson;

/**
 * webAgent serverTime 事件请求格式:
 * List<String> serviceNames</>
 *
 * agent getServerTimeResp事件返回格式: (time的时间到秒)
 * socketId:serviceName:ip:time
 *
 * server处理方式
 * 将agent返回构造serverTimeInfo
 */
public class ServerTimeInfo {

    private String socketId;
    private String ip;
    private String serviceName;
    private long time;

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


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

