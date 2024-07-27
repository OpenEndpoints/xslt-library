package com.offerready.xslt;

import com.databasesandlife.util.gwtsafe.ConfigurationException;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Element;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.databasesandlife.util.DomParser.*;

/**
 * These are passed to the XSLT processor when the XSLT is executed.
 *    <p>
 * These are taken from <code>&lt;placeholder-value placeholder-name="foo" value="10"/&gt;</code>
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
        for (var p : getSubElements(outputDefnElement, "placeholder-value")) {
            var key = getMandatoryAttribute(p, "placeholder-name");
            var value = getMandatoryAttribute(p, "value");
            var language = getOptionalAttribute(p, "language", "");
            paramsForLanguage.putIfAbsent(language, new HashMap<>());
            paramsForLanguage.get(language).put(key, value);
        }
    }
    
    public @Nonnull Map<String, String> get(@CheckForNull String language) {
        var result = new HashMap<>(paramsForLanguage.get(""));
        var lang = paramsForLanguage.get(language);
        if (lang != null) result.putAll(lang);
        return result;
    }
}
