package com.staynest.fb.client;

import com.staynest.fb.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "frontdesk-service")
public interface FrontDeskServiceClient {

    @PostMapping("/api/stay-records/{stayId}/folio-items")
    ApiResponse<?> postFolioItem(@PathVariable Integer stayId, @RequestBody Map<String, Object> folioItem);
}