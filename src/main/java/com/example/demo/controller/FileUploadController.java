package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller; 
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.UserRepository.FileRepository;
import com.example.demo.model.FileEntity;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.PlagiarismDetectionService;

@Controller
@RequestMapping("/user")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PlagiarismDetectionService plagiarismDetectionService;
    
    @Autowired
    private FileRepository fileRepository;

    @Value("${upload.directory}")
    private String uploadDirPath;

    @GetMapping("/upload")
    public String showUploadPage() {
        return "user_upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model, Principal principal) {
        // Validate file type
        String fileType = file.getContentType();
        if (!isValidFileType(fileType)) {
            model.addAttribute("error", "Invalid file type. Only .docx, .pdf, and .txt files are allowed.");
            return "user_upload";
        }
        
        if (file.getSize() > 500 * 1024 * 1024) {
            model.addAttribute("error", "File is too large. Max size is 500MB.");
            return "user_upload";
        }
        try {
            // Ensure the pending folder exists (e.g., uploads/pending)
            String pendingFolderPath = uploadDirPath + "/pending";
            File pendingFolder = new File(pendingFolderPath);
            if (!pendingFolder.exists()) {
                pendingFolder.mkdirs();
            }
            
            // Generate a unique file name (pass file.getOriginalFilename() to the method)
            String pendingFileName = fileStorageService.generateUniqueFileName(file.getOriginalFilename());
            
            // Save the file into the pending folder
            File pendingFile = new File(pendingFolder, pendingFileName);
            file.transferTo(pendingFile);
            
            // Read the stored file’s content and wrap it into a new MultipartFile
            byte[] fileBytes = Files.readAllBytes(pendingFile.toPath());
            MultipartFile storedFileAsMultipart = new MockMultipartFile(
                    file.getName(), pendingFile.getName(), file.getContentType(), fileBytes);
            
            // Retrieve approved files from the main uploads folder (only files, excluding directories)
            File approvedFolder = new File(uploadDirPath);
            File[] approvedFilesArray = approvedFolder.listFiles(f -> f.isFile());
            
            String originalFilename = file.getOriginalFilename();
            List<File> approvedFiles;
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".txt")) {
                approvedFiles = (approvedFilesArray != null)
                    ? Arrays.stream(approvedFilesArray)
                            .filter(f -> f.getName().toLowerCase().endsWith(".txt"))
                            .collect(Collectors.toList())
                    : Collections.emptyList();
            } else {
                approvedFiles = (approvedFilesArray != null)
                    ? Arrays.asList(approvedFilesArray)
                    : Collections.emptyList();
            }
            
            // Compute maximum similarity percentage using the stored file
            double similarity = plagiarismDetectionService.getMaxSimilarity(storedFileAsMultipart, approvedFiles);
            double percentage = similarity * 100;
            
            // Create and persist a new FileEntity record
            String uploadedBy = (principal != null) ? principal.getName() : null;
            FileEntity fileEntity = new FileEntity(pendingFileName, pendingFile.getAbsolutePath(), uploadedBy);
            fileEntity.setPlagiarismPercentage(percentage);
            fileRepository.save(fileEntity);
            
            model.addAttribute("fileId", pendingFileName);
            model.addAttribute("plagiarismPercentage", String.format("%.2f", percentage));
            model.addAttribute("isPlagiarized", similarity >= 0.8);
            
            // If plagiarism is detected, also add the matching sentences to the model.
            // Define local threshold (since THRESHOLD is private in the service)
            final double THRESHOLD = 0.8;
            if (similarity >= THRESHOLD) {
                List<String> matchingSentences = plagiarismDetectionService.getMatchingSentences(storedFileAsMultipart, approvedFiles);
                model.addAttribute("matchingSentences", matchingSentences);
            }
            
            return "plagiarism_result";
        } catch (IOException e) {
            model.addAttribute("error", "File upload failed: " + e.getMessage());
            return "user_upload";
        }
    }

    @PostMapping("/message")
    public String submitUserMessage(@RequestParam("fileId") String fileId,
                                    @RequestParam("message") String message,
                                    Model model) {
        try {
            boolean updated = fileStorageService.updateUserMessage(fileId, message);
            if (updated) {
                model.addAttribute("message", "Message submitted successfully for file: " + fileId);
            } else {
                model.addAttribute("error", "File not found: " + fileId);
            }
            return "request_confirmation";
        } catch (Exception e) {
            model.addAttribute("error", "Error submitting message: " + e.getMessage());
            return "request_confirmation";
        }
    }

    // Method to validate file type
    private boolean isValidFileType(String fileType) {
        return fileType != null && switch (fileType) {
            case "application/pdf",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "text/plain" -> true;
            default -> false;
        };
    }
}
