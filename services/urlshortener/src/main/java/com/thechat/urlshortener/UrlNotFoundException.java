package com.thechat.urlshortener;

import java.util.UUID;

public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("Short URL not found: " + shortCode);
    }

    public UrlNotFoundException(UUID id) {
        super("URL not found: " + id);
    }
}
