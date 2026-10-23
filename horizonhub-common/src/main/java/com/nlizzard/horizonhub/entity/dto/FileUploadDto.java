package com.nlizzard.horizonhub.entity.dto;

public class FileUploadDto {
    private String localPath; // 文件存放路径
    private String originalFilename; // 原始文件名

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

}
