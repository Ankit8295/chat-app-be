package com.thechat.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        String name,
        String refreshName,
        boolean secure,
        String sameSite,
        String path,
        /** Optional cookie Domain (e.g. ".ankitdev.in"). Empty/blank = host-only cookie. */
        String domain
) {
}
