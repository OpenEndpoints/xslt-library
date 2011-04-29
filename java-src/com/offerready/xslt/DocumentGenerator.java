package com.offerready.xslt;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.databasesandlife.util.MD5Hex;
import com.databasesandlife.util.Timer;
import com.offerready.xslt.DocumentOutputDefinition.OutputConversion;
import com.offerready.xslt.WeaklyCachedXsltTransformer.DocumentTemplateInvalidException;
import com.offerready.xslt.WeaklyCachedXsltTransformer.Xslt;
import com.offerready.xslt.WeaklyCachedXsltTransformer.XsltCompilationThreads;

/**
 * An object capable of generating a document for a particular XSLT file.
 *    <p>
 * This object provides does more than just execute XSLT.
 *    <p>
 * <b>Stylevision hacks.</b> It has hacks to change the XSLT produced by StyleVision into that which can be accepted by SAXON.
 *    <p>
 * <b>Conversion after XSLT.</b> After the XSLT is applied, the resulting XML can be further processed.
 * XML-FO to PDF, convert HTML to XML, and convert XML to JSON.
 *    <p>
 * <b>Caching.</b> A DocumentGenerator "retains" the XSLT transformer (weakly cached by WeaklyCachedXsltTransformer).
 * Therefore, a DocumentGenerator should be added to data structures which represent "all documents which can be generated",
 * to avoid XSLT being re-compiled every time a document is requested. 
 * A DocumentGenerator (or "all documents which can be generated") should not be added to e.g. Wicket sessions as
 * these cannot be serialized (and would be big even if they could.)
 */
public class DocumentGenerator {
    
    protected final DocumentOutputDefinition defn;
    protected final WeaklyCachedXsltTransformer transformer;
    protected File fopBaseDirOrNull = null, fopConfigOrNull = null, imagesBase = null;
    
    public static class StyleVisionXslt implements Xslt {
        public final File xsltFile;
        public StyleVisionXslt(File x) { xsltFile = x; }
        @Override public String calculateCacheKey() { return MD5Hex.md5(xsltFile); }
        @Override public Document parseDocument() {
            try {
                DocumentBuilderFactory builderFact = DocumentBuilderFactory.newInstance();
                builderFact.setNamespaceAware(true);
                DocumentBuilder domFactory = builderFact.newDocumentBuilder();
                Document result = domFactory.parse(xsltFile); // DOM object
        
                // XSLT files produced by Stylevision have <xsl:import-schema schema-location="profile-report.xsd"/>
                // The free version of Saxon throws if this tag is present (tells one to buy the commercial version)
                // We don't need XSD checking, so firstly parse the XSLT file into a DOM, then strip out this tag
        
                NodeList allImportSchemaTags = result.getElementsByTagNameNS(
                    "http://www.w3.org/1999/XSL/Transform", "import-schema");
                if (allImportSchemaTags.getLength() > 0) {
                    Element importSchemaTag = (Element) allImportSchemaTags.item(0);
                    importSchemaTag.getParentNode().removeChild(importSchemaTag);
                }
        
                // XSLT files produced with <xsl:result-document href="xxx"> and this causes file to be written,
                // meaning that we can't stream the result to the browser. If we remove the attribute, all is good.
        
                NodeList allResultDocumentTags = result.getElementsByTagNameNS(
                    "http://www.w3.org/1999/XSL/Transform", "result-document");
                for (int i = 0; i < allResultDocumentTags.getLength(); i++) {
                    NamedNodeMap a = allResultDocumentTags.item(i).getAttributes();
                    if (a.getNamedItem("href") != null) a.removeNamedItem("href");
                }
                
                return result;
            }
            catch (SAXException e) { throw new RuntimeException(e); }
            catch (ParserConfigurationException e) { throw new RuntimeException(e); }
            catch (IOException e) { throw new RuntimeException(e); }
        }
    }
    
    public DocumentGenerator(XsltCompilationThreads threads, final DocumentOutputDefinition defn) {
        this.defn = defn;
        if (defn.xsltFileOrNull == null)
            this.transformer = WeaklyCachedXsltTransformer.getIdentityTransformer();
        else 
            this.transformer = WeaklyCachedXsltTransformer.getTransformerOrScheduleCompilation(
                threads, defn.xsltFileOrNull.getAbsolutePath(), new StyleVisionXslt(defn.xsltFileOrNull));
    }
    
    public void setFopConfigOrNull(File fopBaseDirOrNull, File fopConfigOrNull) {
        this.fopBaseDirOrNull = fopBaseDirOrNull;
        this.fopConfigOrNull = fopConfigOrNull;
    }

    public void setImagesBase(File imagesBase) {
        this.imagesBase = imagesBase;
    }

    protected void writePlainXml(DocumentGenerationDestination response, Document xml) throws IOException {
        try {
            Properties systemProperties = System.getProperties();
            systemProperties.remove("javax.xml.transform.TransformerFactory");
            System.setProperties(systemProperties);
            
            response.setContentType("text/plain; charset=UTF-8");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(response.getOutputStream());
            transformer.transform(source, result);
            response.getOutputStream().close();
        }
        catch (TransformerConfigurationException e) { throw new RuntimeException(e); }
        catch (TransformerException e) { throw new RuntimeException(e); }
    }

    protected void writePdfFromXmlFo(OutputStream pdf, Document fo) {
        try (Timer t = new Timer("Create PDF from XML-FO")) {
            // Get a FOP instance (can convert XML-FO into PDF)
            FopFactory fopFactory = FopFactory.newInstance();
            if (fopBaseDirOrNull != null) fopFactory.setFontBaseURL(fopBaseDirOrNull.toURI().toString());
            if (fopConfigOrNull != null) fopFactory.setUserConfig(fopConfigOrNull);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            if (imagesBase != null) foUserAgent.setBaseURL(imagesBase.toURI().toString());
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdf);

            // Setup JAXP using identity transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(new DOMSource(fo), res);
        }
        catch (TransformerException e) { throw new RuntimeException(e); }
        catch (FOPException e) { throw new RuntimeException(e); }
        catch (MalformedURLException e) { throw new RuntimeException(e); }
        catch (IOException e) { throw new RuntimeException(e); }
        catch (SAXException e) { throw new RuntimeException(e); }
    }

    public void assertTemplateValid() throws DocumentTemplateInvalidException {
        if (transformer != null) transformer.assertValid();
    }
    
    /**
     * @param response  this is closed by this method 
     * @param transform if false, then don't do transformation, but output XML instead (for debugging)
     * @throws DocumentTemplateInvalidException in case the XSLT template wasn't valid
     */
    public void transform(DocumentGenerationDestination response, Document xml, boolean transform)
    throws DocumentTemplateInvalidException {
        try {
            if (transform == false) {
                writePlainXml(response, xml);
                return;
            }
            
            Transformer xslt = transformer.newTransformer();
            for (Entry<String, String> placeholderValue : defn.placeholderValues.entrySet())
                xslt.setParameter(placeholderValue.getKey(), placeholderValue.getValue());

            switch (defn.outputConversion) {
                case xmlToJson:
                    response.setContentType((defn.contentType == null ? "application/json" : defn.contentType) + "; charset=UTF-8");
                    StringWriter xmlOutput = new StringWriter();
                    try (Timer t = new Timer("XSLT Transformation")) { xslt.transform(new DOMSource(xml), new StreamResult(xmlOutput)); }
                    JSONObject json = XML.toJSONObject(xmlOutput.toString());
                    OutputStream outputStream = response.getOutputStream();
                    outputStream.write(json.toString(2).getBytes(Charset.forName("UTF-8")));
                    outputStream.close();
                    break;
                    
                case xmlFoToPdf:
                    response.setContentType(defn.contentType == null ? "application/pdf" : defn.contentType);
                    DOMResult xmlFo = new DOMResult();
                    try (Timer t = new Timer("XSLT Transformation to XML-FO")) { xslt.transform(new DOMSource(xml), xmlFo); }
                    writePdfFromXmlFo(response.getOutputStream(), (Document) xmlFo.getNode());
                    break;
                    
                case excelXmlToExcelBinary:
                case excelXmlToExcelBinaryMagicNumbers:
                    response.setContentType(defn.contentType == null ? "application/ms-excel" : defn.contentType);
                    boolean magicNumbers = defn.outputConversion == OutputConversion.excelXmlToExcelBinaryMagicNumbers;
                    try (Timer t = new Timer("XSLT Transformation")) {
                        xslt.transform(new DOMSource(xml), new SAXResult(new ExcelGenerator(magicNumbers, response.getOutputStream())));
                    }
                    break;
       
                default:
                    response.setContentType(defn.contentType == null ? "text/plain" : defn.contentType);
                    StreamResult result = new StreamResult(response.getOutputStream());
                    try (Timer t = new Timer("XSLT Transformation")) { xslt.transform(new DOMSource(xml), result); }
                    break;
            }
    
            response.getOutputStream().close();
        }
        catch (TransformerException e) { throw new RuntimeException(e); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}
