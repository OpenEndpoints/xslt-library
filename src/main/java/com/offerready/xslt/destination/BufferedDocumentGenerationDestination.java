package com.offerready.xslt.destination;

import lombok.Getter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Rather than streaming the output, this stores the result in itself so it can be accessed multiple times.
 */
public class BufferedDocumentGenerationDestination implements DocumentGenerationDestination {

    protected @Getter String contentType;
    protected @Getter @CheckForNull String filenameOrNull = null;
    protected @Getter ByteArrayOutputStream body = null;

    @Override public void setContentType(@Nonnull String contentType) { this.contentType = contentType; }
    @Override public void setContentDispositionToDownload(@CheckForNull String filename) { this.filenameOrNull = filename; }
    @Override public @Nonnull OutputStream getOutputStream() { return body = new ByteArrayOutputStream(); }
    
}
