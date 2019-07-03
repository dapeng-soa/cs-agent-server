package com.github.dapeng.socket.entity;

import java.util.List;

/**
 * 升级部署传输结构体
 */
public class DeployVo {

    /**
     * k8s namespace
     */
    private String nameSpace;

    /**
     * yml文件内容
     */
    private String fileContent;

    /**
     * k8s-yml文件内容
     */
    private String k8sYamlContent;

    /**
     * 服务名
     */
    private String serviceName;
    /**
     * 文件的最后更新时间
     */
    private Long lastModifyTime;
    /**
     * 发送至哪个服务器
     */
    private String ip;
    /**
     * 挂载于容器的文件内容
     */
    List<VolumesFile> volumesFiles;

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getK8sYamlContent() {
        return k8sYamlContent;
    }

    public void setK8sYamlContent(String k8sYamlContent) {
        this.k8sYamlContent = k8sYamlContent;
    }

    public Long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<VolumesFile> getVolumesFiles() {
        return volumesFiles;
    }

    public void setVolumesFiles(List<VolumesFile> volumesFiles) {
        this.volumesFiles = volumesFiles;
    }
}
