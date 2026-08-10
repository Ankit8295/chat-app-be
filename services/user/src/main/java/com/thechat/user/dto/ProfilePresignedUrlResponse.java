package com.thechat.user.dto;

import java.util.Map;
import java.util.UUID;

public record ProfilePresignedUrlResponse(
        UUID mediaId,
        String objectKey,
        String uploadUrl,
        String method,
        long expiresIn,
        Map<String, String> headers) {

}
