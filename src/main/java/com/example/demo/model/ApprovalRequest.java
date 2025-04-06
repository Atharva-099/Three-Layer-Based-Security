package com.example.demo.model;

public class ApprovalRequest {

    private String fileId;
    private double plagiarismPercentage;
    private String reason;
    private String username;
    private String status;  // Added status field

    // Constructor
    public ApprovalRequest(String fileId, double plagiarismPercentage, String reason, String username, String status) {
        this.fileId = fileId;
        this.plagiarismPercentage = plagiarismPercentage;
        this.reason = reason;
        this.username = username;
        this.status = status;  // Initialize status
    }

    // Getters and setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public double getPlagiarismPercentage() {
        return plagiarismPercentage;
    }

    public void setPlagiarismPercentage(double plagiarismPercentage) {
        this.plagiarismPercentage = plagiarismPercentage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
