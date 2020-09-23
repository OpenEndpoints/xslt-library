package com.offerready.xslt;

import com.databasesandlife.util.gwtsafe.ConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.w3c.dom.Element;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.databasesandlife.util.DomParser.getMandatoryAttribute;
import static com.databasesandlife.util.DomParser.getOptionalAttribute;
import static com.databasesandlife.util.DomParser.getSubElements;

/**
 * These are passed to the XSLT processor when the XSLT is executed.
 *    <p>
 * These are taken from <code>&lt;placeholder-value placeholder-name="CostPerUserAlertTreshold" value="10"/&gt;</code>
 * entries in the configuration file.
 *    <p>
 * These can have localized versions for different languages.
 * For each <code>&lt;placeholder-value&gt;</code> element in the config file, there may be additional ones with the same
 * "placeholder-name", but with a "language" attribute, for example 
 * <code>&lt;placeholder-value placeholder-name="CostPerUserAlertTreshold" value="10" language="de"/&gt;</code>.
 */
@RequiredArgsConstructor
public class XsltParameters implements Serializable {
    
    /** From language to a set of params. The language "" (empty string) is the default */
    final protected Map<String, Map<String, String>> paramsForLanguage;
    
    public XsltParameters(@Nonnull Element outputDefnElement) 
    throws ConfigurationException {
        paramsForLanguage = new HashMap<>();
        paramsForLanguage.put("", new HashMap<>());
        for (Element p : getSubElements(outputDefnElement, "placeholder-value")) {
            String key = getMandatoryAttribute(p, "placeholder-name");
            String value = getMandatoryAttribute(p, "value");
            String language = getOptionalAttribute(p, "language", "");
            paramsForLanguage.putIfAbsent(language, new HashMap<>());
            paramsForLanguage.get(language).put(key, value);
        }
    }
    
    public @Nonnull Map<String, String> get(@CheckForNull String language) {
        val result = new HashMap<String, String>(paramsForLanguage.get(""));
        val lang = paramsForLanguage.get(language);
        if (lang != null) result.putAll(lang);
        return result;
    }
}
