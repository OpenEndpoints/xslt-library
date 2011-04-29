package com.offerready.xslt.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Various things such as errors, display names, can exist in multiple languages.
 * The text always exists in a "default language", and then can have zero or more translations into other languages.
 */
@SuppressWarnings("serial")
public class LocalizableText implements Serializable {

    protected String defaultText;
    protected Map<String, String> translations; // key is language 2-letter lowercase code

    public LocalizableText(String d, Map<String,String> t) {
        defaultText = d;
        translations = t;
    }

    public String getText(String lang) {
        String result = translations.get(lang);
        if (result == null) result = defaultText;
        return result;
    }
}
