package com.offerready.xslt.config;

import java.io.File;

import org.w3c.dom.Element;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.EmailTransaction.MxSmtpConfiguration;
import com.databasesandlife.util.EmailTransaction.SmtpServerAddress;
import com.databasesandlife.util.EmailTransaction.SmtpServerConfiguration;
import com.databasesandlife.util.EmailTransaction.TlsSmtpServerAddress;
import com.databasesandlife.util.gwtsafe.ConfigurationException;

public class SmtpServerConfigurationParser extends DomParser {
    
    public static SmtpServerConfiguration parse(File file) throws ConfigurationException {
        Element root = from(file);

        if ( ! root.getNodeName().equals("smtp-configuration")) throw new ConfigurationException("Root node must be <smtp-configuration>");
        assertNoOtherElements(root, "mx-address", "server", "username", "password", "port");
        
        Element mxAddressElement = getOptionalSingleSubElement(root, "mx-address");
        if (mxAddressElement != null) {
            MxSmtpConfiguration result = new MxSmtpConfiguration();
            result.mxAddress = mxAddressElement.getTextContent();
            return result;
        }
        
        SmtpServerAddress result;
        
        if (getOptionalSingleSubElement(root, "username") != null) result = new TlsSmtpServerAddress();
        else result = new SmtpServerAddress();
        
        result.host = getMandatorySingleSubElement(root, "server").getTextContent();

        Element port = getOptionalSingleSubElement(root, "port");
        if (port != null) try { result.port = Integer.parseInt(port.getTextContent()); }
        catch (NumberFormatException e) { throw new ConfigurationException("<port>", e); }
        
        if (getOptionalSingleSubElement(root, "username") != null) {
            TlsSmtpServerAddress r = (TlsSmtpServerAddress) result;
            r.username = getMandatorySingleSubElement(root, "username").getTextContent();
            r.password = getMandatorySingleSubElement(root, "password").getTextContent();
        }
        
        return result;
    }
}
