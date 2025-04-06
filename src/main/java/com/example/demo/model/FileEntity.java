package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;
    private String uploadedBy;
    
    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.PENDING;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    // New field to temporarily store user's explanation message
    private String temporaryUserMessage;

    // New field to store plagiarism percentage
    private Double plagiarismPercentage;

    // New field to store the MAC key generated upon admin approval
    private String macKey;

    public FileEntity() {}

    public FileEntity(String fileName, String filePath, String uploadedBy) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    // Getter and Setter for temporaryUserMessage
    public String getTemporaryUserMessage() {
        return temporaryUserMessage;
    }

    public void setTemporaryUserMessage(String temporaryUserMessage) {
        this.temporaryUserMessage = temporaryUserMessage;
    }

    // Getter and Setter for plagiarismPercentage
    public Double getPlagiarismPercentage() {
        return plagiarismPercentage;
    }

    public void setPlagiarismPercentage(Double plagiarismPercentage) {
        this.plagiarismPercentage = plagiarismPercentage;
    }

    // Getter and Setter for macKey
    public String getMacKey() {
        return macKey;
    }

    public void setMacKey(String macKey) {
        this.macKey = macKey;
    }

    // Updated setter: actually set the filePath
    public void setFilePath(String absolutePath) {
        this.filePath = absolutePath;
    }
}
