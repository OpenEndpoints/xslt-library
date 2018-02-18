package com.offerready.xslt;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

public class StreamingHttpResponseDocumentGenerationDestination implements DocumentGenerationDestination {
    
    protected @Nonnull HttpServletResponse response;
    protected boolean outputStarted = false; // Java silently ignores setting headers after content started, we make it non-silent
    
    public StreamingHttpResponseDocumentGenerationDestination(@Nonnull HttpServletResponse response) {
        this.response = response;
    }

    @Override public void setContentType(@Nonnull String contentType) {
        if (outputStarted) throw new IllegalStateException("Cannot set headers after content started");
        response.setContentType(contentType);
    }
    
    @Override public void setContentDispositionToDownload(@CheckForNull String filename) {
        if (outputStarted) throw new IllegalStateException("Cannot set headers after content started");
        if (filename == null) {
            response.setHeader("content-disposition", "attachment");
        } else {
            if ( ! filename.matches("[\\w\\.\\-]+")) throw new RuntimeException("Filename '" + filename + "' invalid");
            response.setHeader("content-disposition", "attachment; filename=\"" + filename + "\"");
        }
    }

    @Override public @Nonnull OutputStream getOutputStream() {
        outputStarted = true;
        try {
            return response.getOutputStream();
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}
