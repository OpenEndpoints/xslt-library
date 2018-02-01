package com.offerready.xslt;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class StreamingHttpResponseDocumentGenerationDestination implements DocumentGenerationDestination {
    
    protected HttpServletResponse response;
    protected boolean outputStarted = false; // Java silently ignores setting headers after content started, we make it non-silent
    
    public StreamingHttpResponseDocumentGenerationDestination(HttpServletResponse response) {
        this.response = response;
    }

    @Override public void setContentType(String contentType) {
        if (outputStarted) throw new IllegalStateException("Cannot set headers after content started");
        response.setContentType(contentType);
    }
    
    @Override public void setContentDispositionToDownload(String filename) {
        if (outputStarted) throw new IllegalStateException("Cannot set headers after content started");
        if (filename == null) {
            response.setHeader("content-disposition", "attachment");
        } else {
            if ( ! filename.matches("[\\w\\.\\-]+")) throw new RuntimeException("Filename '" + filename + "' invalid");
            response.setHeader("content-disposition", "attachment; filename=\"" + filename + "\"");
        }
    }

    @Override public OutputStream getOutputStream() {
        outputStarted = true;
        try {
            return response.getOutputStream();
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}
