package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.UserRepository.FileRepository;
import com.example.demo.UserRepository.UserRepository;
import com.example.demo.model.ApprovalRequest;
import com.example.demo.model.FileEntity;
import com.example.demo.model.FileStatus;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserLoginRequest;
import com.example.demo.model.UserSignUpRequest;
import com.example.demo.service.ApprovalRequestCache;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApprovalRequestCache approvalRequestCache;
    
    @Autowired
    private FileRepository fileRepository;

    @GetMapping("/")
    public String Home() {
        return "Home";
    }

    @GetMapping("/auth/register")
    public String showSignupPage(Model model) {
        model.addAttribute("userSignupRequest", new UserSignUpRequest());
        return "signup";
    }

    @PostMapping("/auth/register")
    @SuppressWarnings("CallToPrintStackTrace")
    public String registerUser(@ModelAttribute UserSignUpRequest userSignupRequest, Model model) {
        try {
            User user = new User(
                userSignupRequest.getUsername(),
                passwordEncoder.encode(userSignupRequest.getPassword()),
                userSignupRequest.getEmail(),
                Role.USER);
            userRepository.save(user);
            return "redirect:/auth/login";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/auth/login")
    public String showLoginPage(Model model) {
        model.addAttribute("userLoginRequest", new UserLoginRequest());
        return "login";
    }

    // Updated endpoint: List approved files on user home page (only files that physically exist)
    @GetMapping("/user/home")
    public String userHome(Model model) {
        List<FileEntity> approvedFiles = fileRepository.findByStatus(FileStatus.APPROVED)
                .stream()
                .filter(fe -> new File(fe.getFilePath()).exists())
                .collect(Collectors.toList());
        model.addAttribute("approvedFiles", approvedFiles);
        return "user_home";
    }

    @PostMapping("/user/requestApproval")
    public String requestApproval(@RequestParam("fileId") String fileId,
                                  @RequestParam("plagiarismPercentage") double plagiarismPercentage,
                                  @RequestParam("reason") String reason,
                                  Model model,
                                  Principal principal) {
        String username = principal.getName();
        ApprovalRequest request = new ApprovalRequest(fileId, plagiarismPercentage, reason, username, username);
        approvalRequestCache.addApprovalRequest(fileId, request);
        
        model.addAttribute("message", "Your approval request for file " + fileId + " has been sent.");
        return "request_confirmation";
    }
    
    // Updated endpoint: Secure file access via MAC key with handling for different file types
    @PostMapping("/user/viewFile")
    public String viewFile(@RequestParam("fileId") String fileId,
                           @RequestParam("macKey") String macKey,
                           Model model) {
        FileEntity file = fileRepository.findByFileName(fileId);
        if (file != null && file.getMacKey() != null && file.getMacKey().equals(macKey)) {
            String lowerName = file.getFileName().toLowerCase();
            if (lowerName.endsWith(".txt")) {
                try {
                    Path path = Paths.get(file.getFilePath());
                    String content = Files.readString(path);
                    model.addAttribute("fileContent", content);
                    model.addAttribute("fileName", fileId);
                } catch (IOException ex) {
                    model.addAttribute("error", "Error reading file content: " + ex.getMessage());
                    List<FileEntity> approvedFiles = fileRepository.findByStatus(FileStatus.APPROVED);
                    model.addAttribute("approvedFiles", approvedFiles);
                    return "user_home";
                }
                return "file_view";
            } else if (lowerName.endsWith(".pdf") || lowerName.endsWith(".docx")) {
                model.addAttribute("fileDownloadLink", "/user/downloadFile?fileId=" + fileId);
                model.addAttribute("fileName", fileId);
                return "file_download";
            } else {
                model.addAttribute("error", "Unsupported file type for viewing.");
                List<FileEntity> approvedFiles = fileRepository.findByStatus(FileStatus.APPROVED);
                model.addAttribute("approvedFiles", approvedFiles);
                return "user_home";
            }
        } else {
            model.addAttribute("error", "Invalid MAC key for file: " + fileId);
            List<FileEntity> approvedFiles = fileRepository.findByStatus(FileStatus.APPROVED);
            model.addAttribute("approvedFiles", approvedFiles);
            return "user_home";
        }
    }
    
    // New endpoint: Stream the file as a download
    @GetMapping("/user/downloadFile")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") String fileId) {
        FileEntity file = fileRepository.findByFileName(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        Path path = Paths.get(file.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
        
        String contentType = "application/octet-stream";
        try {
            String probe = Files.probeContentType(path);
            if (probe != null) {
                contentType = probe;
            }
        } catch (IOException e) {
            // Fallback to default content type
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
