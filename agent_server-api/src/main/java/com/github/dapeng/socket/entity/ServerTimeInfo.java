package com.github.dapeng.socket.entity;

import com.google.gson.Gson;

public class ServerTimeInfo {

    private String socketId;
    private String ip;
    private long time;

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

