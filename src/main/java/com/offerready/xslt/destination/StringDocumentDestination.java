package com.offerready.xslt.destination;

import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class StringDocumentDestination extends BufferedDocumentGenerationDestination {
    
    protected final Pattern charsetSuffix = Pattern.compile("; charset=utf-8", Pattern.CASE_INSENSITIVE);
    
    protected void assertValid() {
        if ( ! charsetSuffix.matcher(contentType).find())
            throw new RuntimeException("contentType '"+contentType+"' does not contain '"+charsetSuffix+"'");
    }
    
    public @Nonnull String getContentType() {
        assertValid();
        return charsetSuffix.matcher(contentType).replaceAll("");
    }
    
    @Override
    @SneakyThrows(UnsupportedEncodingException.class)
    public String toString() {
        assertValid();
        return body.toString(StandardCharsets.UTF_8.name());
    }
}
