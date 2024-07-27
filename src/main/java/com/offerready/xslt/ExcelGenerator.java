package com.offerready.xslt;

import com.databasesandlife.util.Timer;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.Number;
import jxl.write.*;
import jxl.write.biff.CellValue;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelGenerator extends DefaultHandler {

    @SuppressWarnings("unused") // referenced via valueOf(..) from config file parser
    public enum InputDecimalSeparator {
        dot {
            public @CheckForNull Double tryParseNumber(@Nonnull String str) {
                try { return Double.valueOf(str.replace(",","")); }
                catch (NumberFormatException ignored) { return null; }
            }
            public int determineDecimalPlaces(@Nonnull String string) {
                if (string.contains(".")) return string.trim().length() - string.trim().lastIndexOf(".") - 1;
                else return 0;
            }
        },
        comma {
            public @CheckForNull Double tryParseNumber(@Nonnull String str) {
                try { return Double.valueOf(str.replace(".", "").replace(",", ".")); }
                catch (NumberFormatException ignored) { return null; }
            }
            public int determineDecimalPlaces(@Nonnull String string) {
                if (string.contains(",")) return string.trim().length() - string.trim().lastIndexOf(",") - 1;
                else return 0;
            }
        },
        magic {
            public @CheckForNull Double tryParseNumber(@Nonnull String str) {
                Matcher matcherDecimal = Pattern.compile("(-?[\\d,.']+)[,.](\\d{2})").matcher(str);
                if (matcherDecimal.matches()) {
                    try { 
                        return Double.valueOf(matcherDecimal.group(1).replaceAll("[,.']", "") 
                            + "." + matcherDecimal.group(2)); 
                    }
                    catch (NumberFormatException ignored) { }
                }

                Matcher matcherInteger = Pattern.compile("(-?[\\d,.']+)").matcher(str);
                if (matcherInteger.matches()) {
                    try { 
                        return Double.valueOf(matcherInteger.group(1).replaceAll("[,.']", "")); 
                    }
                    catch (NumberFormatException ignored) { }
                }

                return null;
            }
            public int determineDecimalPlaces(@Nonnull String string) {
                return (string.matches("\\s*-?[\\d',.]*[.,]\\d{2}\\s*")) ? 2 : 0;
            }
        };
        public abstract @CheckForNull Double tryParseNumber(@Nonnull String potentialNumber);
        public abstract int determineDecimalPlaces(@Nonnull String string);
    }

    // Cannot use the underlying Colour directly as it has no equals/hashcode methods
    @SuppressWarnings("unused") // referenced via valueOf(..) from config file parser
    public enum Color {
        green {
            public Colour toExcelColour() { return Colour.GREEN; }
        },
        red {
            public Colour toExcelColour() { return Colour.RED; }
        },
        orange {
            public Colour toExcelColour() { return Colour.ORANGE; }
        };
        public abstract Colour toExcelColour();
    }

    @EqualsAndHashCode
    protected static class CellFormat {
        public boolean isCentered = false;
        public boolean isBold = false;
        public boolean hasTopBorder = false;
        public @CheckForNull Color color = null;
    }

    protected record CellAndNumberFormat(
        @Nonnull CellFormat format,
        @CheckForNull String numberFormat
    ) {
        @SneakyThrows(WriteException.class)
        public @Nonnull WritableCellFormat newFormat() {
            final WritableCellFormat result;
            if (numberFormat != null) result = new WritableCellFormat(new NumberFormat(numberFormat));
            else result = new WritableCellFormat();

            if (format.isCentered) result.setAlignment(Alignment.CENTRE);
            if (format.hasTopBorder) result.setBorder(Border.TOP, BorderLineStyle.THIN);

            var font = new WritableFont(WritableFont.createFont(result.getFont().getName()), result.getFont().getPointSize());
            if (format.isBold) font.setBoldStyle(WritableFont.BOLD);
            if (format.color != null) font.setColour(format.color.toExcelColour());
            result.setFont(font);

            return result;
        }
    }
    
    protected static class CellFromHtml {
        public int colspan = 1;
        public @Nonnull CellFormat format = new CellFormat();
        public boolean forceText = false;
        public @Nonnull StringBuilder string = new StringBuilder();
    }
    
    // Configuration
    protected @Nonnull InputDecimalSeparator inputDecimalSeparator;
    
    // Connection to Excel
    protected @Nonnull WritableWorkbook workbook;
    protected @Nonnull WritableSheet excelSheet;

    // Intermediate store of values
    protected int nextRowInExcel = 0;
    protected @Nonnull List<Integer> maxCharsSeenInColumn = new ArrayList<>();
    protected List<List<CellFromHtml>> currentHeadMatrix=null, currentFootMatrix=null, currentBodyMatrix=null, currentMatrix=null;
    protected List<CellFromHtml> currentRow=null;
    protected CellFromHtml currentCell=null;
    protected int tableDepth = 0;
    protected boolean inScript = false;
    
    // Debugging and logging
    Timer timer;
    
    /** @param xls is closed after transformation */
    @SneakyThrows(IOException.class)
    public ExcelGenerator(@Nonnull InputDecimalSeparator inputDecimalSeparator, @Nonnull OutputStream xls) {
        this.inputDecimalSeparator = inputDecimalSeparator;

        workbook = Workbook.createWorkbook(xls);
        excelSheet = workbook.createSheet("Report", 0);
    }
    
    /** @return String or Double */
    protected @Nonnull Object parseString(@Nonnull String str) {
        var numberOrNull = inputDecimalSeparator.tryParseNumber(str);
        if (numberOrNull != null) return numberOrNull;

        return str.trim();
    }

    protected @Nonnull String getNumberFormat(int decimalPlaces) {
        StringBuilder f = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            f.append(".");
            f.append("0".repeat(decimalPlaces));
        }
        return f.toString();
    }

    @SneakyThrows(WriteException.class)
    protected void writeMatrixToExcel(@Nonnull List<List<CellFromHtml>> matrix) {
        // If we generate a new WritableCellFormat for each cell, at some point we get the error:
        //    Warning:  Maximum number of format records exceeded.  Using default format.
        // Therefore, cache them
        var formats = new HashMap<CellAndNumberFormat, WritableCellFormat>();

        for (var row : matrix) {
            int colIdx = 0;
            for (var cell : row) {
                var cellValue = cell.forceText ? cell.string.toString() : parseString(cell.string.toString());
                int columnWidthChars = 0;
                CellValue excelCell;
                if (cellValue instanceof Double d) {
                    int decimalPlaces = inputDecimalSeparator.determineDecimalPlaces(cell.string.toString());
                    var cellAndNumberFormat = new CellAndNumberFormat(cell.format, getNumberFormat(decimalPlaces));
                    var format = formats.computeIfAbsent(cellAndNumberFormat, CellAndNumberFormat::newFormat);
                    excelCell = new Number(colIdx, nextRowInExcel, d, format);
                    columnWidthChars = String.format("%."+decimalPlaces+"f", d).length();
                } else if (cellValue instanceof String s) {
                    var cellAndNumberFormat = new CellAndNumberFormat(cell.format, null);
                    var format = formats.computeIfAbsent(cellAndNumberFormat, CellAndNumberFormat::newFormat);
                    excelCell = new Label(colIdx, nextRowInExcel, s, format);
                    columnWidthChars = s.length();
                } else {
                    throw new RuntimeException("Unreachable: " + cellValue.getClass());
                }

                while (maxCharsSeenInColumn.size() <= colIdx) maxCharsSeenInColumn.add(0);
                if (columnWidthChars > maxCharsSeenInColumn.get(colIdx)) maxCharsSeenInColumn.set(colIdx, columnWidthChars);

                excelSheet.addCell(excelCell);
                excelSheet.mergeCells(colIdx, nextRowInExcel, (colIdx += cell.colspan) - 1, nextRowInExcel);
            }
            nextRowInExcel++;
        }
    }
    
    @Override public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("table".equals(qName)) {
            tableDepth++;
            if (tableDepth == 1) {
                currentHeadMatrix = new ArrayList<>();
                currentFootMatrix = new ArrayList<>();
                currentBodyMatrix = currentMatrix = new ArrayList<>();
            }
        }
        if (tableDepth != 1) return;
        if ("script".equals(qName)) inScript = true;
        if ("thead".equals(qName)) currentMatrix = currentHeadMatrix;
        if ("tfoot".equals(qName)) currentMatrix = currentFootMatrix;
        if ("tr".equals(qName)) currentMatrix.add(currentRow = new ArrayList<>());
        if ("td".equals(qName) || "th".equals(qName)) {
            currentRow.add(currentCell = new CellFromHtml());
            String colspan = attributes.getValue("colspan");
            if (colspan != null) currentCell.colspan = Integer.parseInt(colspan);
            String style = attributes.getValue("style");
            if (style != null) {
                currentCell.format.isCentered = style.matches(".*text-align:\\s*center.*");
                currentCell.format.isBold = style.matches(".*font-weight:\\s*bold.*");
                currentCell.format.hasTopBorder = style.contains("border-top:");

                Matcher colorMatcher = Pattern.compile("color:\\s*(\\w+)").matcher(style);
                if (colorMatcher.find()) {
                    try { currentCell.format.color = Color.valueOf(colorMatcher.group(1)); }
                    catch (IllegalArgumentException ignored) { } // if user writes "color:purple", just ignore it
                }
            }
            if ("text".equals(attributes.getValue("excel-type"))) currentCell.forceText = true;
        }
    }
  
    @Override public void endElement(String uri, String localName, String qName) {
        if ("table".equals(qName)) {
            if (tableDepth == 1) {
                writeMatrixToExcel(currentHeadMatrix);
                writeMatrixToExcel(currentBodyMatrix);
                writeMatrixToExcel(currentFootMatrix);
            }
            tableDepth--;
        }
        if (tableDepth != 1) return;
        if ("script".equals(qName)) inScript = false;
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
        if (inScript) return;
        if (currentCell != null) {
            String chars = new String(ch, start, length);
            chars = chars.replace("\u00A0", " "); // Non-breaking spaces aren't desired (trim(), later, removes only normal space)
            currentCell.string.append(chars);
        }
    }
    
    @Override
    public void startDocument() {
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
        catch (IOException | WriteException e) { throw new SAXException(e); }
    }

    @SneakyThrows({ParserConfigurationException.class, IOException.class})
    public static void writeExcelBinaryFromExcelXml(@Nonnull InputDecimalSeparator inputDecimalSeparator, @Nonnull OutputStream xls, @Nonnull InputStream xml) {
        try {
            ExcelGenerator handler = new ExcelGenerator(inputDecimalSeparator, xls);
            SAXParserFactory.newInstance().newSAXParser().parse(xml, handler);
        }
        catch (SAXException e) { throw new RuntimeException("Input XML to conversion to XLS process is not valid", e); }
    }
}
