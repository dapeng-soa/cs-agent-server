package com.github.dapeng.socket.entity;

public class YamlServiceVo {

    //gitURL: String, gitName: String, serviceName: String, buildOperation: String
    private String gitURL = "";
    private String gitName = "";
    private String serviceName = "";
    private String buildOperation = "";
    private String branchName = "master";


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

    @Override
    public boolean equals(Object obj) {
        if (obj == null ) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof YamlServiceVo) {
            YamlServiceVo tmp = (YamlServiceVo)obj;
            if (tmp.serviceName != null && tmp.serviceName.trim().equals(this.serviceName)
                    && tmp.gitName != null && tmp.gitName.trim().equals(this.gitName)
                    && tmp.branchName != null && tmp.branchName.trim().equals(this.branchName)
                    && tmp.buildOperation != null && tmp.branchName.trim().equals(this.buildOperation)
                    && tmp.gitURL != null && tmp.gitURL.trim().equals(this.gitURL)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
