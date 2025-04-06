package com.example.demo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.UserRepository.FileRepository;
import com.example.demo.model.FileEntity;

@Service
public class FileStorageService {
    //Change the directory accordingly.
    private final String UPLOAD_DIR = "D:/demo/uploads/";

    @Autowired
    private FileRepository fileRepository;
    
    public String storeFile(MultipartFile file) throws IOException {
    
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        Path filePath = Path.of(UPLOAD_DIR + uniqueFileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        String extractedText = extractText(filePath.toFile(), file.getOriginalFilename());

        return "File uploaded successfully: " + filePath.toString() + "\nExtracted Text:\n" + extractedText;
    }
    
    // Retrieve all stored files
    public List<File> getStoredFiles() {
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists() || !directory.isDirectory()) {
            return List.of();
        }
        return List.of(directory.listFiles());
    }

    // Generate a unique file name to prevent overwriting existing files
    public String generateUniqueFileName(String originalFileName) {
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return UUID.randomUUID().toString() + fileExtension;
    }

    // Extract text based on file type (.docx, .pdf, .txt)
    private String extractText(File file, String fileName) throws IOException {
        if (fileName.endsWith(".docx")) {
            return extractTextFromDocx(file);
        } else if (fileName.endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (fileName.endsWith(".txt")) {
            return extractTextFromTxt(file);
        }
        return "Unsupported file type for text extraction.";
    }

    // Extract text from .docx files
    private String extractTextFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            List<String> paragraphs = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.toList());
            return String.join("\n", paragraphs);
        }
    }

    // Extract text from .pdf files
    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            return new PDFTextStripper().getText(document);
        }
    }

    // Extract text from .txt files
    private String extractTextFromTxt(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
    
    // Updated method: update the FileEntity with the user's message
    public boolean updateUserMessage(String fileId, String message) {
        FileEntity fileEntity = fileRepository.findByFileName(fileId);
        if (fileEntity != null) {
            fileEntity.setTemporaryUserMessage(message);
            fileRepository.save(fileEntity);
            return true;
        }
        return false;
    }

    // Implement getFileEntityByFileName to return the FileEntity using fileRepository
    public FileEntity getFileEntityByFileName(String fileName) {
        return fileRepository.findByFileName(fileName);
    }
}
