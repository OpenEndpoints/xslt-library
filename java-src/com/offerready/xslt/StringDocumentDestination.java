package com.offerready.xslt;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public class StringDocumentDestination extends BufferedDocumentGenerationDestination {
    
    protected final Pattern charsetSuffix = Pattern.compile("; charset=utf-8", Pattern.CASE_INSENSITIVE);
    
    protected void assertValid() {
        if ( ! charsetSuffix.matcher(contentType).find())
            throw new RuntimeException("contentType '"+contentType+"' does not contain '"+charsetSuffix+"'");
    }
    
    public String getContentType() {
        assertValid();
        return charsetSuffix.matcher(contentType).replaceAll("");
    }
    
    @Override
    public String toString() {
        try {
            assertValid();
            return body.toString("UTF-8");
        }
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }
}
