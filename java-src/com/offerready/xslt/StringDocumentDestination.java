package com.offerready.xslt;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class StringDocumentDestination implements DocumentGenerationDestination {

    protected String contentType = "";
    protected ByteArrayOutputStream stream = new ByteArrayOutputStream();
    
    public byte[] toByteArray() { return stream.toByteArray(); }
    public String getContentType() { return contentType; }
    
    /** Assumes that the bytes are written to it in UTF-8 */
    public String toString() { 
        try { return stream.toString("UTF-8"); }
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }
    
    @Override public void setContentType(String contentType) { this.contentType = contentType; }
    @Override public void setContentDispositionToDownload(String filename) { }
    @Override public OutputStream getOutputStream() { return stream; }
}
