package com.offerready.xslt.parser;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.Timer;
import com.databasesandlife.util.gwtsafe.ConfigurationException;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/** Parses XML which has &lt;security&gt; containing a set of &lt;secret-key&gt; elements. */
public class SecurityParser extends DomParser {

    /** @return not empty */
    @SuppressWarnings("TryWithIdenticalCatches") 
    public static @Nonnull String[] parse(@Nonnull InputStream i)
    throws ConfigurationException {
        try (var ignored = new Timer("parse-security-xml")) {
            var doc = DomParser.newDocumentBuilder().parse(i);

            var root = doc.getDocumentElement();
            if ( ! root.getNodeName().equals("security")) throw new ConfigurationException("Root node must be <security>");

            assertNoOtherElements(root, "secret-key");
            
            var result = new ArrayList<String>();
            for (var e : getSubElements(root, "secret-key")) result.add(e.getTextContent());
            if (result.isEmpty()) throw new ConfigurationException("At least one <secret-key> must be present");
            return result.toArray(new String[0]);
        }
        catch (SAXException e) { throw new ConfigurationException(e); }  // invalid XML, is a configuration problem
        catch (IOException e) { throw new ConfigurationException(e); }  // can be throws if malformed UTF-8 etc.
    }

    /** @return not empty */
    public static @Nonnull String[] parse(@Nonnull File file)
    throws ConfigurationException {
        try {
            try (var i = new FileInputStream(file)) { return parse(i); }
        }
        catch (IOException e) { throw new ConfigurationException("Problem with '"+file+"'", e); }
    }
}
