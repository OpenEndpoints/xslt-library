package com.offerready.xslt;

import lombok.Getter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class BufferedDocumentGenerationDestination implements DocumentGenerationDestination {

    protected @Getter String contentType;
    protected @Getter @CheckForNull String filenameOrNull = null;
    protected @Getter ByteArrayOutputStream body = null;

    @Override public void setContentType(@Nonnull String contentType) { this.contentType = contentType; }
    @Override public void setContentDispositionToDownload(@CheckForNull String filename) { this.filenameOrNull = filename; }
    @Override public @Nonnull OutputStream getOutputStream() { return body = new ByteArrayOutputStream(); }
    
}
