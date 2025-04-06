package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller; 
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.UserRepository.FileRepository;
import com.example.demo.UserRepository.UserRepository;
import com.example.demo.model.ApprovalRequest;
import com.example.demo.model.FileEntity;
import com.example.demo.model.FileStatus;
import com.example.demo.model.User;
import com.example.demo.service.ApprovalRequestCache;
import com.example.demo.service.EmailService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.MacKeyService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Value("${upload.directory}")
    private String uploadDirPath;

    @Autowired
    private MacKeyService macKeyService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ApprovalRequestCache approvalRequestCache;
    
    @Autowired
    private FileStorageService fileStorageService;

    // Show admin dashboard view with list of pending files (awaiting approval) and approval requests
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAdminDashboard(Model model) {
        // Fetch files from the pending folder only (files awaiting admin approval)
        String pendingFolderPath = uploadDirPath + "/pending";
        File pendingFolder = new File(pendingFolderPath);
        String[] fileNames;
        if (!pendingFolder.exists()) {
            fileNames = new String[]{"No files found."};
        } else {
            fileNames = pendingFolder.list((dir, name) -> !name.startsWith("."));
            if (fileNames == null || fileNames.length == 0) {
                fileNames = new String[]{"No files found."};
            }
        }
        
        // Fetch approval requests from cache (if any)
        Map<String, ApprovalRequest> approvalRequests = approvalRequestCache.getAllApprovalRequests();

        // For each pending file, retrieve its details (e.g., plagiarism percentage, user reason) from the database
        Map<String, FileEntity> fileDetails = new HashMap<>();
        for (String f : fileNames) {
            String trimmedFileName = f.trim();
            FileEntity fe = fileStorageService.getFileEntityByFileName(trimmedFileName);
            if (fe != null) {
                fileDetails.put(trimmedFileName, fe);
            }
        }

        model.addAttribute("files", fileNames);
        model.addAttribute("approvalRequests", approvalRequests);
        model.addAttribute("fileDetails", fileDetails);

        return "admin_dashboard";
    }

    // Approve a file: move from pending folder to final uploads folder, update status, update file path, generate MAC key, and notify user
    @PostMapping("/review/approve/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<String> approveFile(@PathVariable String fileName, @RequestParam String username) {
        String pendingFolderPath = uploadDirPath + "/pending";
        File pendingFile = new File(pendingFolderPath + "/" + fileName);
        if (!pendingFile.exists()) {
            return ResponseEntity.status(404).body("File not found in pending folder: " + pendingFile.getAbsolutePath());
        }

        // Move file from pending folder to final uploads folder
        File finalFile = new File(uploadDirPath + "/" + fileName);
        try {
            Files.move(pendingFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to move file to final location: " + e.getMessage());
        }

        // Update the FileEntity record: set status to APPROVED and update file path to the new location
        FileEntity fileEntity = fileStorageService.getFileEntityByFileName(fileName);
        if (fileEntity != null) {
            fileEntity.setStatus(FileStatus.APPROVED);
            fileEntity.setFilePath(finalFile.getAbsolutePath());
            // Generate MAC key and store it in the record
            String macKey = macKeyService.generateMacKey();
            fileEntity.setMacKey(macKey);
            fileRepository.save(fileEntity);

            // Generate MAC key email notification
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(404).body("User not found for username: " + username);
            }

            String userEmail = userOptional.get().getEmail();
            String subject = "File Approved - MAC Key Notification";
            String body = "Your file '" + fileName + "' has been approved.\nYour MAC key: " + macKey;
            try {
                emailService.sendEmail(userEmail, subject, body);
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Failed to send email: " + ex.getMessage());
            }
        }

        return ResponseEntity.ok("File " + fileName + " approved successfully. MAC Key has been emailed to the user.");
    }

    // Reject a file: delete from pending folder and remove its record from the database
    @PostMapping("/review/reject/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectFile(@PathVariable String fileName) {
        String pendingFolderPath = uploadDirPath + "/pending";
        File pendingFile = new File(pendingFolderPath + "/" + fileName);
        if (!pendingFile.exists()) {
            return ResponseEntity.status(404).body("File not found in pending folder: " + pendingFile.getAbsolutePath());
        }

        if (pendingFile.delete()) {
            // Also remove the corresponding FileEntity record from the database
            FileEntity fileEntity = fileStorageService.getFileEntityByFileName(fileName);
            if (fileEntity != null) {
                fileRepository.delete(fileEntity);
            }
            return ResponseEntity.ok("File " + fileName + " rejected, deleted, and record removed.");
        } else {
            return ResponseEntity.status(500).body("Failed to delete the file.");
        }
    }

    // Admin login page endpoint
    @GetMapping("/login")
    public String showAdminLoginPage() {
        return "adminlogin"; // Corresponds to adminlogin.html
    }

    // Admin login form submission endpoint
    @PostMapping("/login")
    public String adminLogin(@RequestParam("username") String username,
                             @RequestParam("password") String password) {
        // Implement admin authentication logic if needed.
        // On successful login, redirect to the admin dashboard view.
        return "redirect:/admin/dashboard";
    }
}
