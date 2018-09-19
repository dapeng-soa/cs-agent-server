package com.github.dapeng.socket.entity;

import java.util.ArrayList;
import java.util.List;

public class BuildVo {

    private String buildServerIp;

    private List<YamlServiceVo> buildServices = new ArrayList<>();

    public String getBuildServerIp() {
        return buildServerIp;
    }

    public void setBuildServerIp(String buildServerIp) {
        this.buildServerIp = buildServerIp;
    }

    public List<YamlServiceVo> getBuildServices() {
        return buildServices;
    }

    public void setBuildServices(List<YamlServiceVo> buildServices) {
        this.buildServices = buildServices;
    }
}
