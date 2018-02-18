package com.offerready.xslt;

import lombok.val;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

public class EmailPartDocumentDestination extends BufferedDocumentGenerationDestination {

    public @Nonnull BodyPart getBodyPart() {
        try {
            val dataSource = new DataSource() {
                @Override public String getContentType() { return contentType; }
                @Override public InputStream getInputStream() { return new ByteArrayInputStream(body.toByteArray()); }
                @Override public String getName() { return filenameOrNull; }
                @Override public OutputStream getOutputStream() { throw new RuntimeException("unreachable"); }
            };

            val filePart = new MimeBodyPart();
            filePart.setDataHandler(new DataHandler(dataSource));
            if (filenameOrNull != null) {
                filePart.setFileName(filenameOrNull);
                filePart.setDisposition(Part.ATTACHMENT);
            }
            
            return filePart;
        }
        catch (MessagingException e) { throw new RuntimeException(e); }
    }
}
