package com.offerready.xslt;

import com.offerready.xslt.ExcelGenerator.InputDecimalSeparator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.Serializable;

@SuppressWarnings("serial")
public class DocumentOutputDefinition implements Serializable {

    public enum OutputConversion { none, xmlToJson, xslFoToPdf, excelXmlToExcelBinary };
    
    public @CheckForNull File xsltFileOrNull;
    public @Nonnull XsltParameters xsltParameters;
    public @Nonnull OutputConversion outputConversion = OutputConversion.none;
    public @Nonnull InputDecimalSeparator inputDecimalSeparator = InputDecimalSeparator.dot;
    public @CheckForNull String contentType;

    public DocumentOutputDefinition(@Nonnull XsltParameters xsltParameters) {
        this.xsltParameters = xsltParameters;
    }
}
