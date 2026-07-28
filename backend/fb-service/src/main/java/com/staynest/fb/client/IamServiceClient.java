package com.staynest.fb.client;

import com.staynest.fb.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "IAM-SERVICE")
public interface IamServiceClient {

    // Returns the user (404 if it doesn't exist) so a dining reservation can validate its guestId.
    @GetMapping("/api/users/{id}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable Integer id);
}
