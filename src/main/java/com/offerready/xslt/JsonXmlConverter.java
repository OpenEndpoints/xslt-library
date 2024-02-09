package com.offerready.xslt;

import lombok.extern.slf4j.Slf4j;
import org.json.*;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.databasesandlife.util.DomParser.newDocumentBuilder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Matcher.quoteReplacement;
import static java.util.stream.Collectors.toMap;

/**
 * Converts between JSON and XML.
 *   <p>
 * The default libraries use "content":"xyz" for the body of XML tags, we use "_content"
 * so that JSON with actual "content":"xyz" can be parsed into &lt;content&gt;xyz&lt;/content&gt;
 */
@Slf4j
public class JsonXmlConverter {

    protected static final XMLParserConfiguration config = XMLParserConfiguration.ORIGINAL.withcDataTagName("_content");
    protected static final Pattern patternFirstChar = Pattern.compile("^[^a-zA-Z_]");
    protected static final Pattern patternNonFirstChar = Pattern.compile("[^a-zA-Z0-9-_.]");
    
    protected @Nonnull String makeKeySafeForXml(@Nonnull Pattern pattern, @Nonnull String key) {
        var result = new StringBuilder();
        var matcher = pattern.matcher(key);
        while (matcher.find()) {
            var c = matcher.group();
            assert c.length() == 1;
            matcher.appendReplacement(result, quoteReplacement(String.format("_%04x_", (int) c.toCharArray()[0])));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Alter JSON to have keys which are safe as XML element names.
     * JSON objects can have keys such as "foo bar" but XML elements cannot have keys such as &lt;foo bar&gt;.
     * The default JSON-to-XML converter simply converts JSON object keys to XML element names verbatim.
     */
    protected @Nonnull Object makeKeysInJsonSafeForXml(@Nonnull Object input) {
        if (input instanceof JSONObject) 
            return new JSONObject(((JSONObject) input).keySet().stream().collect(toMap(
                key -> makeKeySafeForXml(patternNonFirstChar, makeKeySafeForXml(patternFirstChar, key)),
                key -> makeKeysInJsonSafeForXml(((JSONObject) input).opt(key))
            )));
        else if (input instanceof JSONArray) 
            return new JSONArray(StreamSupport.stream(((JSONArray) input).spliterator(), false)
                .map(e -> makeKeysInJsonSafeForXml(e))
                .toList());
        else return input;
    }

    public @Nonnull Element convertJsonToXml(@Nonnull Reader json, @Nonnull String rootElement)
    throws JSONException, IOException {
        var jsonObject = new JSONTokener(json).nextValue();
        var jsonObjectWithSafeKeys = makeKeysInJsonSafeForXml(jsonObject);
        var xmlString = XML.toString(jsonObjectWithSafeKeys, rootElement, config);

        try {
            return newDocumentBuilder().parse(new InputSource(new StringReader(xmlString))).getDocumentElement();
        }
        catch (SAXException e) {
            log.info("JSON converted to XML: " + xmlString);
            throw new JSONException(e);
        }
    }

    public @Nonnull Element convertJsonToXml(@Nonnull String contentType, @Nonnull InputStream jsonInputStream, @Nonnull String rootElement)
    throws JSONException, IOException {
        var charset = com.google.common.net.MediaType.parse(contentType).charset().or(UTF_8);
        try (var reader = new InputStreamReader(jsonInputStream, charset)) {
            return convertJsonToXml(reader, rootElement);
        }
    }

    public @Nonnull String convertXmlToJson(@Nonnull String xml) {
        var json = XML.toJSONObject(xml, config);
        return json.toString(2);
    }
}
