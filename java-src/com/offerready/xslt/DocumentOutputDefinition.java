package com.offerready.xslt;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class DocumentOutputDefinition implements Serializable {
    
    public enum OutputConversion { none, xmlToJson, xmlFoToPdf, excelXmlToExcelBinary, excelXmlToExcelBinaryMagicNumbers }; 
    
    public File xsltFileOrNull;
    public Map<String, String> placeholderValues;
    public OutputConversion outputConversion = OutputConversion.none;
    /** can be null */ public String contentType;
}
