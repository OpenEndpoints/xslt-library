package com.offerready.xslt.parser;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.gwtsafe.ConfigurationException;
import com.offerready.xslt.DocumentOutputDefinition;
import com.offerready.xslt.DocumentOutputDefinition.OutputConversion;
import com.offerready.xslt.ExcelGenerator.InputDecimalSeparator;
import com.offerready.xslt.XsltParameters;
import org.w3c.dom.Element;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;

/**
 * Parsers which parses document-definition files for any XSLT-style document generation application.
 */
public class DocumentOutputDefinitionParser extends DomParser {

    public static @CheckForNull String parseOptionalDownloadFilename(@Nonnull Element outputDefnElement) throws ConfigurationException {
        String downloadFilename = null;
        var downloadFilenameElement = getOptionalSingleSubElement(outputDefnElement, "download-filename");
        if (downloadFilenameElement != null) {
            downloadFilename = downloadFilenameElement.getTextContent();
            if ( ! downloadFilename.matches("[\\w.\\-]+"))
                throw new ConfigurationException("<download-filename>" + downloadFilename + "</download-filename> is invalid: " +
                    "only A-Z, a-z, 0-9, '_', '-', '.' are allowed, due to problems with inexact HTTP specification " +
                    "w.r.t. character sets and download file names");
        }
        return downloadFilename;
    }
    
    /**
     * @param templateContainerDirectory the directory which will contain "xyz/report.xslt" for "xslt-directory"="xyz"
     */
    public static @Nonnull DocumentOutputDefinition parseDocumentOutputDefinition(
        @Nonnull File templateContainerDirectory, @Nonnull Element outputDefnElement
    ) throws ConfigurationException {
        // xslt-directory is legacy, prefer xslt-file
        final File xsltFileOrNull;
        var xsltFileEl = getOptionalSingleSubElement(outputDefnElement, "xslt-file");
        var xsltDirEl = getOptionalSingleSubElement(outputDefnElement, "xslt-directory");
        if (xsltFileEl != null) {
            xsltFileOrNull = new File(templateContainerDirectory, getMandatoryAttribute(xsltFileEl, "name"));
            if ( ! xsltFileOrNull.isFile()) throw new ConfigurationException("XSLT File '" + xsltFileOrNull + "' not found");
        } else if (xsltDirEl != null) {
            File dir = new File(templateContainerDirectory, getMandatoryAttribute(xsltDirEl, "name"));
            xsltFileOrNull = new File(dir, "report.xslt");
            if ( ! xsltFileOrNull.isFile()) throw new ConfigurationException("XSLT File '" + xsltFileOrNull + "' not found");
        } else xsltFileOrNull = null;
        
        String contentType = null;
        var contentTypeElement = getOptionalSingleSubElement(outputDefnElement, "content-type");
        if (contentTypeElement != null) contentType = getMandatoryAttribute(contentTypeElement, "type");
        
        var result = new DocumentOutputDefinition(new XsltParameters(outputDefnElement));
        result.xsltFileOrNull = xsltFileOrNull;
        result.outputConversion =
            getSubElements(outputDefnElement, "convert-output-xml-to-json").size() > 0 ? OutputConversion.xmlToJson :
            getSubElements(outputDefnElement, "convert-output-xml-fo-to-pdf").size() > 0 ? OutputConversion.xslFoToPdf :  // deprecated
            getSubElements(outputDefnElement, "convert-output-xsl-fo-to-pdf").size() > 0 ? OutputConversion.xslFoToPdf :
            getSubElements(outputDefnElement, "convert-output-xml-to-excel").size()  > 0 ? OutputConversion.excelXmlToExcelBinary :
            OutputConversion.none;
        result.contentType = contentType;

        var excel = getOptionalSingleSubElement(outputDefnElement, "convert-output-xml-to-excel");
        if (excel != null) {
            // Deprecated, use input-decimal-separator attribute instead
            if (Boolean.parseBoolean(getOptionalAttribute(excel, "magic-numbers", "false")))
                result.inputDecimalSeparator = InputDecimalSeparator.magic;

            var style = getOptionalAttribute(excel, "input-decimal-separator");
            if (style != null) result.inputDecimalSeparator = InputDecimalSeparator.valueOf(style);
        }

        return result;
    }
}
