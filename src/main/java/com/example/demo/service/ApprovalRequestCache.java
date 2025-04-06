package com.example.demo.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import com.example.demo.model.ApprovalRequest;

@Component
public class ApprovalRequestCache {
    private final Map<String, ApprovalRequest> cache = new ConcurrentHashMap<>();

    public void addApprovalRequest(String fileId, ApprovalRequest request) {
        cache.put(fileId, request);
    }

    public ApprovalRequest getApprovalRequest(String fileId) {
        return cache.get(fileId);
    }

    public Map<String, ApprovalRequest> getAllApprovalRequests() {
        return cache;
    }

    // Method to update the status of an approval request
    public void updateStatus(String fileId, String status) {
        ApprovalRequest request = cache.get(fileId);
        if (request != null) {
            request.setStatus(status);  // Update the status of the request
        }
    }
}
