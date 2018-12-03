package com.github.dapeng.socket.entity;

import java.util.Objects;

public class DependServiceVo {

    private String gitURL = "";
    private String gitName = "";
    private String serviceName = "";
    private String buildOperation = "";
    private String branchName = "master";
    private String imageName = "";


    public String getGitURL() {
        return gitURL;
    }

    public void setGitURL(String gitURL) {
        this.gitURL = gitURL;
    }

    public String getGitName() {
        return gitName;
    }

    public void setGitName(String gitName) {
        this.gitName = gitName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getBuildOperation() {
        return buildOperation;
    }

    public void setBuildOperation(String buildOperation) {
        this.buildOperation = buildOperation;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public DependServiceVo() {
    }

    @Override
    public int hashCode() {

        return Objects.hash(gitURL, gitName, serviceName, buildOperation, branchName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependServiceVo that = (DependServiceVo) o;
        return Objects.equals(gitURL, that.gitURL) &&
                Objects.equals(gitName, that.gitName) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(buildOperation, that.buildOperation) &&
                Objects.equals(branchName, that.branchName);
    }
}
