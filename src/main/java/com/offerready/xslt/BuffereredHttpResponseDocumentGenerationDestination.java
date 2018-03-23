package com.offerready.xslt;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

public class BuffereredHttpResponseDocumentGenerationDestination extends BufferedDocumentGenerationDestination {
    
    protected int statusCode = HttpServletResponse.SC_OK;
    protected @Nonnull String reasonPhrase = "OK";
    
    public void setStatusCode(int code, @Nonnull String phrase) { statusCode = code; reasonPhrase = phrase; }
    
    @Override
    public void setContentDispositionToDownload(@CheckForNull String filename) {
        if (filename != null && ! filename.matches("[\\w\\.\\-]+")) throw new RuntimeException("Filename '" + filename + "' invalid");
        super.setContentDispositionToDownload(filename);
    }

    @SuppressWarnings("deprecation") // There's no other way to do setStatus(code, phrase)
    @SneakyThrows(IOException.class)
    public void deliver(@Nonnull HttpServletResponse response) {
        response.setStatus(statusCode, reasonPhrase);
        response.setContentType(contentType);

        if (filenameOrNull != null) response.setHeader("content-disposition", "attachment; filename=\"" + filenameOrNull + "\"");

        IOUtils.write(body.toByteArray(), response.getOutputStream());
    }
}
