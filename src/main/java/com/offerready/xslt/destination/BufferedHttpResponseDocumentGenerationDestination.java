package com.offerready.xslt.destination;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 * Stores the result of a transformation inside the object, in a form suitable for delivering to an
 * {@link HttpServletResponse} via the {@link #deliver(HttpServletResponse)} method.
 */
public class BufferedHttpResponseDocumentGenerationDestination extends BufferedDocumentGenerationDestination {

    protected @Getter int statusCode = HttpServletResponse.SC_OK;
    protected @CheckForNull String reasonPhrase = null;
    protected @CheckForNull URL redirectUrl = null;

    public void setStatusCode(int code) { statusCode = code; reasonPhrase = null; }
    public void setStatusCode(int code, @Nonnull String phrase) { statusCode = code; reasonPhrase = phrase; }

    public void setRedirectUrl(@Nonnull URL redirectUrl) {
        this.redirectUrl = redirectUrl;
        this.statusCode = HttpServletResponse.SC_MOVED_PERMANENTLY;
        this.reasonPhrase = null;
    }

    @Override
    public void setContentDispositionToDownload(@CheckForNull String filename) {
        if (filename != null && ! filename.matches("[\\w\\.\\-]+")) throw new RuntimeException("Filename '" + filename + "' invalid");
        super.setContentDispositionToDownload(filename);
    }

    @SuppressWarnings("deprecation") // There's no other way to do setStatus(code, phrase)
    @SneakyThrows(IOException.class)
    public void deliver(@Nonnull HttpServletResponse response) {
        if (redirectUrl != null) {
            response.sendRedirect(redirectUrl.toExternalForm());
            return;
        }

        if (body == null) {
            if (reasonPhrase == null) response.sendError(statusCode);
            else response.sendError(statusCode, reasonPhrase);
        } else {
            if (reasonPhrase == null) response.setStatus(statusCode);
            else response.setStatus(statusCode, reasonPhrase);

            response.setContentType(contentType);

            if (filenameOrNull != null) {
                response.setHeader("content-disposition", "attachment; filename=\"" + filenameOrNull + "\"");
            }

            IOUtils.write(body.toByteArray(), response.getOutputStream());
        }
    }
}
