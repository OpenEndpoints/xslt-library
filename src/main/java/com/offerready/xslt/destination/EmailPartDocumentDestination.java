package com.offerready.xslt.destination;

import lombok.SneakyThrows;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EmailPartDocumentDestination extends BufferedDocumentGenerationDestination {

    protected final @Nonnull BodyPart bodyPart;

    /** @param destination does not have to be populated yet */
    @SneakyThrows(MessagingException.class)
    public static @Nonnull BodyPart newMimeBodyForDestination(BufferedDocumentGenerationDestination destination) {
        var dataSource = new DataSource() {
            @Override public String getContentType() { return destination.getContentType(); }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(destination.getBody().toByteArray()); }
            @Override public String getName() { return destination.getFilenameOrNull(); }
            @Override public OutputStream getOutputStream() { throw new RuntimeException("unreachable"); }
        };

        var result = new MimeBodyPart();
        result.setDataHandler(new DataHandler(dataSource));

        return result;
    }

    public EmailPartDocumentDestination() {
        bodyPart = newMimeBodyForDestination(this);
    }

    @SneakyThrows(MessagingException.class)
    @Override public void setContentDispositionToDownload(@CheckForNull String filename) {
        super.setContentDispositionToDownload(filename);

        if (filenameOrNull == null) {
            bodyPart.setFileName(null);
            bodyPart.setDisposition(null);
        } else {
            bodyPart.setFileName(filenameOrNull);
            bodyPart.setDisposition(Part.ATTACHMENT);
        }
    }

    public @Nonnull BodyPart getBodyPart() { return bodyPart; }
}
