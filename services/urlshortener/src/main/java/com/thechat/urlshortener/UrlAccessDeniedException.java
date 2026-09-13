package com.thechat.urlshortener;

import java.util.UUID;

public class UrlAccessDeniedException extends RuntimeException {

    public UrlAccessDeniedException(UUID id) {
        super("Not allowed to delete URL: " + id);
    }
}
