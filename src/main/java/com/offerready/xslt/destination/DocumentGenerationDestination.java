package com.offerready.xslt.destination;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.OutputStream;

/**
 * Documents (= XSLT Reports) can be generated to a "destination".
 *  <p>
 * A "destination" must be capable of having:
 * <ul>
 * <li>its MIME content type set (e.g. "text/html") with {@link #setContentType(String)},
 * <li>and offering an OutputStream to which the bytes of the document can be sent with {@link #getOutputStream()}.
 * </ul>
 */
public interface DocumentGenerationDestination {
    
    void setContentType(@Nonnull String contentType);

    /** @param filename can be null indicating no particular filename is preferred */
    void setContentDispositionToDownload(@CheckForNull String filename);
    
    @Nonnull OutputStream getOutputStream();

}
