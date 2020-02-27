package com.offerready.xslt.xsltfunction;

import javax.annotation.Nonnull;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Base64 {
    
    public static @Nonnull String encode(@Nonnull String input) {
        return new String(java.util.Base64.getEncoder().encode(input.getBytes(UTF_8)), UTF_8);
    }

    public static @Nonnull String decode(@Nonnull String base64) {
        return new String(java.util.Base64.getDecoder().decode(base64), UTF_8);
    }
}
