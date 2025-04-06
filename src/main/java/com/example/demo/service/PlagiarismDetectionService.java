package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import opennlp.tools.tokenize.SimpleTokenizer;

@Service
public class PlagiarismDetectionService {

    private static final double THRESHOLD = 0.8; // 80% similarity
    private static final Set<String> STOP_WORDS = Set.of("a", "and", "the", "or", "is", "to", "in", "of", "that", "this");

    // Extracts text based on file type
    public String extractText(MultipartFile file) throws IOException {
        String fileType = file.getContentType();

        if (fileType == null) {
            throw new IOException("File type is null.");
        }

        switch (fileType) {
            case "text/plain" -> {
                return new String(file.getBytes());
            }

            case "application/pdf" -> {
                return extractTextFromPDF(file);
            }

            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                return extractTextFromDocx(file);
            }

            default -> throw new IOException("Unsupported file format.");
        }
    }

    // Extract text from PDF
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    // Extract text from DOCX
    private String extractTextFromDocx(MultipartFile file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph para : document.getParagraphs()) {
                text.append(para.getText()).append(" ");
            }
        }
        return text.toString().trim();
    }

    // Helper method to extract text from File (for .txt, .pdf, .docx)
    public String extractTextFromFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) {
            return Files.readString(file.toPath());
        } else if (name.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file)) {
                return new PDFTextStripper().getText(document);
            }
        } else if (name.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file.toPath()))) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    sb.append(p.getText()).append(" ");
                }
                return sb.toString();
            }
        } else {
            return "";
        }
    }

    // Tokenize and remove stop words
    private List<String> preprocessText(String text) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        return Arrays.stream(tokenizer.tokenize(text.toLowerCase()))
                     .filter(word -> !STOP_WORDS.contains(word))
                     .collect(Collectors.toList());
    }

    // Compute Cosine Similarity
    private double computeCosineSimilarity(List<String> text1, List<String> text2) {
        Set<String> allWords = new HashSet<>(text1);
        allWords.addAll(text2);

        RealVector v1 = new ArrayRealVector(allWords.size());
        RealVector v2 = new ArrayRealVector(allWords.size());

        int index = 0;
        for (String word : allWords) {
            v1.setEntry(index, Collections.frequency(text1, word));
            v2.setEntry(index, Collections.frequency(text2, word));
            index++;
        }

        double normProduct = v1.getNorm() * v2.getNorm();
        return (normProduct == 0) ? 0 : (v1.dotProduct(v2) / normProduct);
    }

    // Compare uploaded file with stored files
    public boolean isPlagiarized(MultipartFile file, List<File> storedFiles) throws IOException {
        String uploadedText = extractText(file);
        List<String> uploadedTokens = preprocessText(uploadedText);

        for (File storedFile : storedFiles) {
            String storedText = Files.readString(Paths.get(storedFile.getAbsolutePath()));
            List<String> storedTokens = preprocessText(storedText);

            double similarity = computeCosineSimilarity(uploadedTokens, storedTokens);
            if (similarity >= THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    // Compute maximum similarity percentage against all stored files
    public double getMaxSimilarity(MultipartFile file, List<File> storedFiles) throws IOException {
        String uploadedText = extractText(file);
        List<String> uploadedTokens = preprocessText(uploadedText);
        double maxSimilarity = 0.0;
        for (File storedFile : storedFiles) {
            String storedText = extractTextFromFile(storedFile);
            List<String> storedTokens = preprocessText(storedText);
            double similarity = computeCosineSimilarity(uploadedTokens, storedTokens);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
            }
        }
        return maxSimilarity;
    }
}
