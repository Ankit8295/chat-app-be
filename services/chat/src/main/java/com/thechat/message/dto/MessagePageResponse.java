package com.thechat.message.dto;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> items,
        String prevCursor,
        String nextCursor) {
}
