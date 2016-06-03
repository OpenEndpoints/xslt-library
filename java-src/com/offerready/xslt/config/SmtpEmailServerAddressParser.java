package com.offerready.xslt.config;

import java.io.File;

import org.w3c.dom.Element;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.TlsSmtpEmailTransaction.SmtpEmailServerAddress;
import com.databasesandlife.util.TlsSmtpEmailTransaction.TlsSmtpEmailServerAddress;
import com.databasesandlife.util.gwtsafe.ConfigurationException;

public class SmtpEmailServerAddressParser extends DomParser {
    
    public static SmtpEmailServerAddress parse(File file) throws ConfigurationException {
        Element root = from(file);

        if ( ! root.getNodeName().equals("smtp-configuration")) throw new ConfigurationException("Root node must be <smtp-configuration>");
        assertNoOtherElements(root, "server", "username", "password", "port");
        
        SmtpEmailServerAddress result;
        
        if (getOptionalSingleSubElement(root, "username") != null) result = new TlsSmtpEmailServerAddress();
        else result = new SmtpEmailServerAddress();
        
        result.server = getMandatorySingleSubElement(root, "server").getTextContent();

        Element port = getOptionalSingleSubElement(root, "port");
        if (port != null) try { result.port = Integer.parseInt(port.getTextContent()); }
        catch (NumberFormatException e) { throw new ConfigurationException("<port>", e); }
        
        if (getOptionalSingleSubElement(root, "username") != null) {
            TlsSmtpEmailServerAddress r = (TlsSmtpEmailServerAddress) result;
            r.username = getMandatorySingleSubElement(root, "username").getTextContent();
            r.password = getMandatorySingleSubElement(root, "password").getTextContent();
        }
        
        return result;
    }
}
