package com.offerready.xslt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.Timer;
import com.databasesandlife.util.gwtsafe.ConfigurationException;

/** Parses XML which has &lt;security&gt; containing a set of &lt;secret-key&gt; elements. */
public class SecurityParser extends DomParser {
    
    public static @Nonnull String[] parse(@Nonnull InputStream i)
    throws ConfigurationException {
        try (Timer t = new Timer("parse-security-xml")) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(i);

            Node root = doc.getDocumentElement();
            if ( ! root.getNodeName().equals("security")) throw new ConfigurationException("Root node must be <security>");

            assertNoOtherElements(root, "secret-key");
            
            List<String> result = new ArrayList<String>();
            for (Element e : getSubElements(root, "secret-key")) result.add(e.getTextContent());
            return result.toArray(new String[0]);
        }
        catch (SAXException e) { throw new ConfigurationException(e); }  // invalid XML, is a configuration problem
        catch (IOException e) { throw new ConfigurationException(e); }  // can be throws if malformed UTF-8 etc.
        catch (ParserConfigurationException e) { throw new RuntimeException(e); }
    }

    public static @Nonnull String[] parse(@Nonnull File file)
    throws ConfigurationException {
        try {
            try (InputStream i = new FileInputStream(file)) { return parse(i); }
        }
        catch (IOException e) { throw new ConfigurationException("Problem with '"+file+"'", e); }
    }
}
