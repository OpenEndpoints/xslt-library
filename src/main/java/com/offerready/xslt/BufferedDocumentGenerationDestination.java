package com.offerready.xslt;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public abstract class BufferedDocumentGenerationDestination implements DocumentGenerationDestination {

    protected String contentType;
    protected @CheckForNull String filenameOrNull;
    protected final ByteArrayOutputStream body = new ByteArrayOutputStream();

    @Override public void setContentType(@Nonnull String contentType) { this.contentType = contentType; }
    @Override public void setContentDispositionToDownload(@CheckForNull String filename) { this.filenameOrNull = filename; }
    @Override public @Nonnull OutputStream getOutputStream() { return body; }
    
}
