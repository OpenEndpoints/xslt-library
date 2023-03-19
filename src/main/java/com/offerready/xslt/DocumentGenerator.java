package com.offerready.xslt;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.gwtsafe.ConfigurationException;
import com.offerready.xslt.destination.DocumentGenerationDestination;
import lombok.SneakyThrows;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.databasesandlife.util.MD5Hex;
import com.databasesandlife.util.Timer;
import com.offerready.xslt.WeaklyCachedXsltTransformer.DocumentTemplateInvalidException;
import com.offerready.xslt.WeaklyCachedXsltTransformer.Xslt;
import com.offerready.xslt.WeaklyCachedXsltTransformer.XsltCompilationThreads;

/**
 * An object capable of generating a document for a particular XSLT file.
 *    <p>
 * This object executes XSLT, and in addition:
 *    <p>
 * <ul>
 * <li><b>Stylevision hacks.</b> It has hacks to change the XSLT produced by StyleVision into that which can be accepted by
 * the free version of Saxon called Saxon-HE.
 *    <p>
 * <li><b>Conversion after XSLT.</b> After the XSLT is applied, the resulting XML can be further processed.
 * XSL-FO to PDF, convert HTML to XML, and convert XML to JSON.
 * </ul>
 *    <p>
 * Objects of this class reference the complied XSLT transfomer.
 * As long as you need to access the XSLT, retain a link to this object.
 * Creating a new object might involve the XSLT being re-complied (depending on the cache in {@link WeaklyCachedXsltTransformer}).
 *    <p>
 * A DocumentGenerator (or "all documents which can be generated") should not be added to e.g. Wicket sessions as
 * these cannot be serialized (and would be big even if they could.)
 */
public class DocumentGenerator {
    
    protected final @Nonnull DocumentOutputDefinition defn;
    protected final @Nonnull WeaklyCachedXsltTransformer transformer;
    protected @CheckForNull File fopBaseDirOrNull = null, fopConfigOrNull = null, imagesBase = null;
    
    public static class StyleVisionXslt implements Xslt {
        public final @Nonnull File xsltFile;
        public StyleVisionXslt(@Nonnull File x) { xsltFile = x; }
        @Override public @Nonnull String calculateCacheKey() { return MD5Hex.md5(xsltFile); }

        @SneakyThrows(IOException.class)
        @Override public @Nonnull Document parseDocument() throws ConfigurationException {
            final Document result;
            try { result = DomParser.newDocumentBuilder().parse(xsltFile); } // DOM Object
            catch (SAXException e) { throw new ConfigurationException("XSLT file '" + xsltFile.getAbsolutePath() + "' is not valid XML"); }

            // XSLT files produced by Stylevision have <xsl:import-schema schema-location="profile-report.xsd"/>
            // The free version of Saxon throws if this tag is present (tells one to buy the commercial version)
            // We don't need XSD checking, so firstly parse the XSLT file into a DOM, then strip out this tag

            var allImportSchemaTags = result.getElementsByTagNameNS(
                "http://www.w3.org/1999/XSL/Transform", "import-schema");
            if (allImportSchemaTags.getLength() > 0) {
                var importSchemaTag = (Element) allImportSchemaTags.item(0);
                importSchemaTag.getParentNode().removeChild(importSchemaTag);
            }

            // XSLT files produced with <xsl:result-document href="xxx"> and this causes file to be written,
            // meaning that we can't stream the result to the browser. If we remove the attribute, all is good.

            var allResultDocumentTags = result.getElementsByTagNameNS(
                "http://www.w3.org/1999/XSL/Transform", "result-document");
            for (int i = 0; i < allResultDocumentTags.getLength(); i++) {
                var a = allResultDocumentTags.item(i).getAttributes();
                if (a.getNamedItem("href") != null) a.removeNamedItem("href");
            }

            return result;
        }
    }
    
    public DocumentGenerator(
        @Nonnull XsltCompilationThreads threads, @Nonnull DocumentOutputDefinition defn,
        @Nonnull Function<File, Xslt> newXslt
    )
    throws ConfigurationException {
        this.defn = defn;
        if (defn.xsltFileOrNull == null)
            this.transformer = WeaklyCachedXsltTransformer.getIdentityTransformer();
        else 
            this.transformer = WeaklyCachedXsltTransformer.getTransformerOrScheduleCompilation(
                threads, defn.xsltFileOrNull.getAbsolutePath(), newXslt.apply(defn.xsltFileOrNull));
    }
    
    public DocumentGenerator(@Nonnull XsltCompilationThreads threads, @Nonnull DocumentOutputDefinition defn)
    throws ConfigurationException {
        this(threads, defn, StyleVisionXslt::new);
    }

    public void setFopConfigOrNull(@CheckForNull File fopBaseDirOrNull, @CheckForNull File fopConfigOrNull) {
        this.fopBaseDirOrNull = fopBaseDirOrNull;
        this.fopConfigOrNull = fopConfigOrNull;
    }

    public void setImagesBase(@Nonnull File imagesBase) {
        this.imagesBase = imagesBase;
    }

    @SneakyThrows({TransformerException.class, IOException.class})
    protected void writePlainXml(@Nonnull DocumentGenerationDestination response, @Nonnull Document xml) {
        var systemProperties = System.getProperties();
        systemProperties.remove("javax.xml.transform.TransformerFactory");
        System.setProperties(systemProperties);

        response.setContentType("text/plain; charset=UTF-8");
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        var source = new DOMSource(xml);
        var result = new StreamResult(response.getOutputStream());
        transformer.transform(source, result);
        response.getOutputStream().close();
    }

    @SneakyThrows({TransformerException.class, IOException.class, SAXException.class})
    protected void writePdfFromXslFo(@Nonnull OutputStream pdf, @Nonnull Document fo, @CheckForNull URIResolver uriResolverOrNull) {
        try (var ignored = new Timer("Create PDF from XSL-FO")) {
            // Get a FOP instance (can convert XSL-FO into PDF)
            var fopFactory = FopFactory.newInstance();
            if (fopBaseDirOrNull != null) fopFactory.setFontBaseURL(fopBaseDirOrNull.toURI().toString());
            if (fopConfigOrNull != null) fopFactory.setUserConfig(fopConfigOrNull);
            var foUserAgent = fopFactory.newFOUserAgent();
            if (imagesBase != null) foUserAgent.setBaseURL(imagesBase.toURI().toString());
            if (uriResolverOrNull != null) foUserAgent.setURIResolver(uriResolverOrNull);
            var fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdf);

            // Setup JAXP using identity transformer
            var factory = TransformerFactory.newInstance();
            var transformer = factory.newTransformer(); // identity transformer
            
            // Resulting SAX events (the generated FO) must be piped through to FOP
            var res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(new DOMSource(fo), res);
        }
    }

    public void assertTemplateValid() throws DocumentTemplateInvalidException {
        transformer.assertValid();
    }
    
    /**
     * @param response  this is closed by this method 
     * @param transform if false, then don't do transformation, but output XML instead (for debugging)
     * @param uriResolverOrNull if not null, pass an object which can, for example, fetch or create images via programmatic logic
     * @param language for example "de" to choose different XSLT params (placeholder values). Or null to just use the default.
     */
    @SneakyThrows(IOException.class)
    public void transform(
        @Nonnull DocumentGenerationDestination response, @Nonnull Document xml,
        boolean transform, @CheckForNull URIResolver uriResolverOrNull,
        @CheckForNull String language
    ) throws DocumentTemplateInvalidException, TransformerException {
        if ( ! transform) {
            writePlainXml(response, xml);
            return;
        }

        var xslt = transformer.newTransformer();
        for (var placeholderValue : defn.xsltParameters.get(language).entrySet())
            xslt.setParameter(placeholderValue.getKey(), placeholderValue.getValue());

        switch (defn.outputConversion) {
            case xmlToJson:
                response.setContentType((defn.contentType == null ? "application/json" : defn.contentType) + "; charset=UTF-8");
                var xmlOutput = new StringWriter();
                try (var ignored = new Timer("XSLT Transformation")) { 
                    xslt.transform(new DOMSource(xml), new StreamResult(xmlOutput)); 
                }
                var json = XML.toJSONObject(xmlOutput.toString());
                try (var outputStream = response.getOutputStream()) {
                    outputStream.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                }
                break;

            case xslFoToPdf:
                response.setContentType(defn.contentType == null ? "application/pdf" : defn.contentType);
                var xslFo = new DOMResult();
                try (var ignored = new Timer("XSLT Transformation to XSL-FO")) {
                    xslt.transform(new DOMSource(xml), xslFo); 
                }
                try (var outputStream = response.getOutputStream()) {
                    writePdfFromXslFo(outputStream, (Document) xslFo.getNode(), uriResolverOrNull);
                }
                break;

            case excelXmlToExcelBinary:
                response.setContentType(defn.contentType == null ? "application/ms-excel" : defn.contentType);
                try (var outputStream = response.getOutputStream()) {
                    xslt.transform(new DOMSource(xml), new SAXResult(new ExcelGenerator(defn.inputDecimalSeparator, outputStream)));
                }
                break;

            default:
                response.setContentType((defn.contentType == null ? "text/plain" : defn.contentType) + "; charset=UTF-8");
                try (var outputStream = response.getOutputStream()) {
                    var result = new StreamResult(outputStream);
                    xslt.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
                    try (var ignored = new Timer("XSLT Transformation")) { xslt.transform(new DOMSource(xml), result); }
                }
                break;
        }
    }
}
