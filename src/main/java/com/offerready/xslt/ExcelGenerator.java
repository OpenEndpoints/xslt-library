package com.offerready.xslt;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.CellValue;
import jxl.write.biff.RowsExceededException;

import lombok.val;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.databasesandlife.util.Timer;

public class ExcelGenerator extends DefaultHandler {
    
    protected static class CellFormat {
        public boolean isCentered = false, isBold = false, hasTopBorder = false;
        @Override public int hashCode() { return (isBold ? 345 : 654) 
            * (isCentered ? 2343 : 456436) * (hasTopBorder ? 3453 : 45645); }
        @Override public boolean equals(Object other) {
            if ( ! (other instanceof CellFormat)) return false;
            if (((CellFormat)other).isCentered != isCentered) return false;
            if (((CellFormat)other).isBold != isBold) return false;
            if (((CellFormat)other).hasTopBorder != hasTopBorder) return false;
            return true;
        }
    }
    
    protected static class CellFromHtml {
        public int colspan = 1;
        public @Nonnull CellFormat format = new CellFormat();
        public boolean forceText = false;
        public @Nonnull StringBuilder string = new StringBuilder();
    }
    
    // Configuration
    protected boolean magicNumbers;
    
    // Connection to Excel
    protected @Nonnull WritableWorkbook workbook;
    protected @Nonnull WritableSheet excelSheet;
    
    // Intermediate store of values
    protected int nextRowInExcel = 0;
    protected @Nonnull List<Integer> maxCharsSeenInColumn = new ArrayList<Integer>();
    protected List<List<CellFromHtml>> currentHeadMatrix=null, currentFootMatrix=null, currentBodyMatrix=null, currentMatrix=null;
    protected List<CellFromHtml> currentRow=null;
    protected CellFromHtml currentCell=null;
    protected int tableDepth = 0;
    
    // Debugging and logging
    Timer timer;
    
    /** @param xls is closed after transformation */
    public ExcelGenerator(boolean magicNumbers, @Nonnull OutputStream xls) {
        try {
            this.magicNumbers = magicNumbers;
            
            workbook = Workbook.createWorkbook(xls);
            excelSheet = workbook.createSheet("Report", 0);
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }
    
    /** @return String or Number */
    protected @Nonnull Object parseString(@Nonnull String str) {
        try { return new Double(str); }
        catch (NumberFormatException e) { }
        
        if (magicNumbers) {
            Matcher matcherDecimal = Pattern.compile("(-?[\\d,.]+)[,.](\\d{2})").matcher(str);
            if (matcherDecimal.matches()) 
                try { return new Double(matcherDecimal.group(1).replace(".", "").replace(",", "") + "." + matcherDecimal.group(2)); }
                catch (NumberFormatException e) { } 

            Matcher matcherInteger = Pattern.compile("(-?[\\d,.]+)").matcher(str);
            if (matcherInteger.matches()) 
                try { return new Double(matcherInteger.group(1).replace(".", "").replace(",", "")); }
                catch (NumberFormatException e) { } 
        }
        
        return str.trim();
    }
    
    // If we generate a new WritableCellFormat for each cell, at some point we get the error:
    //    Warning:  Maximum number of format records exceeded.  Using default format.
    // Therefore we have to pre-generate all possible formats in advance.
    protected @Nonnull Map<CellFormat, WritableCellFormat> generateWriteableCellFormats(@Nonnull WritableCellFormat base) {
        try {
            val bold = new WritableFont(WritableFont.createFont(base.getFont().getName()),
                base.getFont().getPointSize(), WritableFont.BOLD);
            
            val result = new HashMap<CellFormat, WritableCellFormat>();
            for (boolean isCentered : new boolean[] { true, false }) {
                for (boolean isBold : new boolean[] { true, false }) {
                    for (boolean hasTopBorder : new boolean[] { true, false }) {
                        val f = new CellFormat();
                        f.isCentered = isCentered;
                        f.isBold = isBold;
                        f.hasTopBorder = hasTopBorder;

                        val format = new WritableCellFormat(base);
                        if (isCentered) format.setAlignment(Alignment.CENTRE);
                        if (isBold) format.setFont(bold);
                        if (hasTopBorder) format.setBorder(Border.TOP, BorderLineStyle.THIN);
        
                        result.put(f, format);
                    }
                }
            }
            return result;
        }
        catch (WriteException e) { throw new RuntimeException(e); }
    }
    
    protected void writeMatrixToExcel(@Nonnull List<List<CellFromHtml>> matrix) {
        try {
            val normalFormat = generateWriteableCellFormats(new WritableCellFormat());
            val twoDecimalPlaces = generateWriteableCellFormats(new WritableCellFormat(new NumberFormat("#,##0.00")));

            for (final List<CellFromHtml> row : matrix) {
                for (int colIdx = 0; colIdx < row.size(); ) {
                    val cell = row.get(colIdx);
                    val cellValue = cell.forceText ? cell.string.toString() : parseString(cell.string.toString());
                    int columnWidthChars = 0;
                    CellValue excelCell;
                    if (cellValue instanceof Double) {
                        Map<CellFormat, WritableCellFormat> format = normalFormat;
                        if (cell.string.toString().matches("\\s*-?[\\d,.]*[.,]\\d{2}\\s*")) format = twoDecimalPlaces;
                        excelCell = new Number(colIdx, nextRowInExcel, (Double) cellValue, format.get(cell.format));
                        columnWidthChars = String.format("%.2f", ((Double) cellValue)).length();
                    } else if (cellValue instanceof String) {
                        excelCell = new Label(colIdx, nextRowInExcel, (String) cellValue, normalFormat.get(cell.format));
                        columnWidthChars = ((String) cellValue).length();
                    } else throw new RuntimeException("Unreachable: " + cellValue.getClass());

                    while (maxCharsSeenInColumn.size() <= colIdx) maxCharsSeenInColumn.add(0);
                    if (columnWidthChars > maxCharsSeenInColumn.get(colIdx)) maxCharsSeenInColumn.set(colIdx, columnWidthChars);

                    excelSheet.addCell(excelCell);
                    excelSheet.mergeCells(colIdx, nextRowInExcel, (colIdx += cell.colspan) - 1, nextRowInExcel);
                }
                nextRowInExcel++;
            }
        }
        catch (RowsExceededException e) { throw new RuntimeException(e); }
        catch (WriteException e) { throw new RuntimeException(e); }
    }
    
    @Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("table".equals(qName)) {
            tableDepth++;
            if (tableDepth == 1) {
                currentHeadMatrix = new ArrayList<>();
                currentFootMatrix = new ArrayList<>();
                currentBodyMatrix = currentMatrix = new ArrayList<>();
            }
        }
        if (tableDepth != 1) return;
        if ("thead".equals(qName)) currentMatrix = currentHeadMatrix;
        if ("tfoot".equals(qName)) currentMatrix = currentFootMatrix;
        if ("tr".equals(qName)) currentMatrix.add(currentRow = new ArrayList<>());
        if ("td".equals(qName) || "th".equals(qName)) {
            currentRow.add(currentCell = new CellFromHtml());
            String colspan = attributes.getValue("colspan");
            if (colspan != null) currentCell.colspan = Integer.parseInt(colspan);
            String style = attributes.getValue("style");
            if (style != null && style.matches(".*text-align:\\s*center.*")) currentCell.format.isCentered = true;
            if (style != null && style.matches(".*font-weight:\\s*bold.*")) currentCell.format.isBold = true;
            if (style != null && style.matches(".*border-top:.*")) currentCell.format.hasTopBorder = true;
            if ("text".equals(attributes.getValue("excel-type"))) currentCell.forceText = true;
        }
    }
  
    @Override public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("table".equals(qName)) {
            if (tableDepth == 1) {
                writeMatrixToExcel(currentHeadMatrix);
                writeMatrixToExcel(currentBodyMatrix);
                writeMatrixToExcel(currentFootMatrix);
            }
            tableDepth--;
        }
        if (tableDepth != 1) return;
        if ("thead".equals(qName)) currentMatrix = currentBodyMatrix;
        if ("tfoot".equals(qName)) currentMatrix = currentBodyMatrix;
        if ("tr".equals(qName)) {
            boolean isEmpty = true;
            for (CellFromHtml cell : currentRow) if (cell.string.length() > 0) isEmpty = false;
            if (isEmpty) currentMatrix.remove(currentMatrix.size()-1);
            currentRow = null;
        }
        if ("td".equals(qName) || "th".equals(qName)) currentCell = null;
    }

    @Override public void characters(char[] ch, int start, int length) throws SAXException {
        if (tableDepth != 1) return;
        if (currentCell != null) {
            String chars = new String(ch, start, length);
            chars = chars.replace("\u00A0", " "); // Non-breaking spaces aren't desired (trim(), later, removes only normal space)
            currentCell.string.append(chars);
        }
    }
    
    @Override
    public void startDocument() throws SAXException {
        timer = new Timer("Create XLS from XML");
    }
    
    @Override public void endDocument() throws SAXException {
        try {
            for (int colIdx = 0; colIdx < maxCharsSeenInColumn.size(); colIdx++) {
                int length = maxCharsSeenInColumn.get(colIdx);
                if (length > 0) excelSheet.setColumnView(colIdx, (int) (length*1.5));       // *1.5 otherwise cols too narrow
            }
            
            workbook.write(); 
            workbook.close();
            timer.close();
        }
        catch (IOException e) { throw new SAXException(e); }
        catch (WriteException e) { throw new SAXException(e); }
    }
    
    public static void writeExcelBinaryFromExcelXml(boolean magicNumbers, @Nonnull OutputStream xls, @Nonnull InputStream xml) {
        try {
            ExcelGenerator handler = new ExcelGenerator(magicNumbers, xls);
            SAXParserFactory.newInstance().newSAXParser().parse(xml, handler);
        }
        catch (SAXException e) { throw new RuntimeException("Input XML to convertion to XLS process is not valid", e); }
        catch (ParserConfigurationException e) { throw new RuntimeException(e); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}
