package com.offerready.xslt;

import lombok.SneakyThrows;
import lombok.val;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

public class EmailPartDocumentDestination extends BufferedDocumentGenerationDestination {

    protected final @Nonnull BodyPart bodyPart;

    @SneakyThrows(MessagingException.class)
    public EmailPartDocumentDestination() {
        val dataSource = new DataSource() {
            @Override public String getContentType() { return contentType; }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(body.toByteArray()); }
            @Override public String getName() { return filenameOrNull; }
            @Override public OutputStream getOutputStream() { throw new RuntimeException("unreachable"); }
        };

        bodyPart = new MimeBodyPart();
        bodyPart.setDataHandler(new DataHandler(dataSource));
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
