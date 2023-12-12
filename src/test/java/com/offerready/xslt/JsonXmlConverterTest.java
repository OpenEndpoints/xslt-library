package com.offerready.xslt;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.StringReader;

import static com.databasesandlife.util.InputOutputStreamUtil.prettyPrintXml;

public class JsonXmlConverterTest extends TestCase {

    public void testMakeKeysSafeForXml() {
        var jsonString = "[ { \"1foo BA\uffffr1\": 123 } ]";
        var json = new JSONTokener(new StringReader(jsonString)).nextValue();
        var safeJson = (JSONArray) new JsonXmlConverter().makeKeysInJsonSafeForXml(json);
        var safeJsonString = safeJson.toString();
        assertEquals("[{\"_0031_foo_0020_BA_ffff_r1\":123}]", safeJsonString);
    }
    
    public void testConvertJsonToXml() throws Exception {
        // Test: converts "content":"xyz" correctly
        var json = "{\"content\":\"äöü\"}";
        var xml = new JsonXmlConverter().convertJsonToXml(new StringReader(json), "root");
        var xmlStr = prettyPrintXml(xml);
        assertEquals("<root>\n   <content>äöü</content>\n</root>", xmlStr);
    }

    public void testConvertXmlToJson() {
        // Test: doesn't ignore <content> tag (which the library does do by default, unfortunately)
        var xml = "<foo><content>The content äöü</content></foo>";
        var json = new JsonXmlConverter().convertXmlToJson(xml);
        assertEquals("{\"foo\": {\"content\": \"The content äöü\"}}", json);
    }
}
