package com.offerready.xslt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.databasesandlife.util.Timer;

/**
 * Performs POST requests and puts the result into a {@link DocumentGenerationDestination}.
 */
@SuppressWarnings("serial")
public class PostRequestClient {
    
    public static class PostFailedException extends Exception {
        public int statusCode;
        public @Nonnull String statusMessage;
        public PostFailedException(int c, @Nonnull String s) {
            super("HTTP returned status code " + c + ": " + s);
            statusCode = c; statusMessage = s;
        }
    }
    
    public interface OutputStreamFiller {
        public void writeToOutputStream(OutputStream o);
    }
    
    /**
     * @param dest is not closed by this method
     * @param bodyContentType if null, GET is done instead of POST 
     */
    @SneakyThrows(IOException.class)
    public void post(
        @Nonnull DocumentGenerationDestination dest, @Nonnull URL url, @Nonnull Map<String, String> getParameters,
        @CheckForNull String bodyContentType, @Nonnull OutputStreamFiller postBody
    ) throws PostFailedException {
        try (var t = new Timer("send-post-request: " + url)) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            if (bodyContentType != null) {
                connection.setRequestProperty("Content-Type", bodyContentType);
                connection.setRequestMethod("POST");
                OutputStream postRequest = connection.getOutputStream();
                postBody.writeToOutputStream(postRequest);
            }
            if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
                LoggerFactory.getLogger(getClass()).info("HTTP request to '" + url + 
                    "' failed: " +connection.getResponseCode() + " " + connection.getResponseMessage());
                throw new PostFailedException(connection.getResponseCode(), connection.getResponseMessage());
            }
            
            String contentDisposition = connection.getHeaderField("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("attachment")) {
                Matcher contentDispositionMatcher = Pattern.compile("filename=['\"](.+?)['\"]").matcher(contentDisposition);
                if (contentDispositionMatcher.find())
                    dest.setContentDispositionToDownload(contentDispositionMatcher.group(1));
            }
            
            dest.setContentType(connection.getContentType());
            
            IOUtils.copy(connection.getInputStream(), dest.getOutputStream());
        }
    }

    /** 
     * @param dest is not closed by this method
     * @param xmlOrNull if null then a GET request is done instead of a POST 
     */
    public void postXml(
        @Nonnull DocumentGenerationDestination dest, @Nonnull URL url, @Nonnull Map<String, String> getParameters, @CheckForNull Element xmlOrNull
    ) throws PostFailedException {
        post(dest, url, getParameters, xmlOrNull == null ? null : "text/xml", new OutputStreamFiller() {
            @SneakyThrows(TransformerException.class)
            public void writeToOutputStream(OutputStream o) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(xmlOrNull);

                StreamResult result = new StreamResult(o);
                transformer.transform(source, result);
            }
        });
    }
}
