package com.offerready.xslt;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public abstract class BufferedDocumentGenerationDestination implements DocumentGenerationDestination {

    protected String contentType;
    protected String filenameOrNull;
    protected ByteArrayOutputStream body = new ByteArrayOutputStream();

    @Override public void setContentType(String contentType) { this.contentType = contentType; }
    @Override public void setContentDispositionToDownload(String filename) { this.filenameOrNull = filename; }
    @Override public OutputStream getOutputStream() { return body; }
    
}
