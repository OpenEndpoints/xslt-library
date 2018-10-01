package com.offerready.xslt;

import com.offerready.xslt.ExcelGenerator.InputDecimalSeparator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class DocumentOutputDefinition implements Serializable {

    public enum OutputConversion { none, xmlToJson, xslFoToPdf, excelXmlToExcelBinary };
    
    public @CheckForNull File xsltFileOrNull;
    public @Nonnull Map<String, String> placeholderValues;
    public @Nonnull OutputConversion outputConversion = OutputConversion.none;
    public @Nonnull InputDecimalSeparator inputDecimalSeparator = InputDecimalSeparator.dot;
    public @CheckForNull String contentType;

    public DocumentOutputDefinition(@Nonnull Map<String, String> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }
}
