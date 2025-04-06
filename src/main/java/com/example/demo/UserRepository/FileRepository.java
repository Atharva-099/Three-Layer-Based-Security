package com.example.demo.UserRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.FileEntity;
import com.example.demo.model.FileStatus;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    // Additional custom methods can be defined here if needed, for example:
    // List<FileEntity> findByUploadedBy(String uploadedBy);

    // Custom method to find a file by its unique file name
    FileEntity findByFileName(String fileName);

    List<FileEntity> findByStatus(FileStatus approved);
}
