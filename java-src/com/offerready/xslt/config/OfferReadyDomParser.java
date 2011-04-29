package com.offerready.xslt.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.gwtsafe.ConfigurationException;

public class OfferReadyDomParser extends DomParser {

    /**
     * @param elementName for example "displayName"
     * @return null if no element of this displayName found
     */
    protected static LocalizableText parseOptionalLocalizableText(Element container, String elementName)
    throws ConfigurationException {
        String defaultLang = null;
        Map<String,String> nonDefaultLang = new HashMap<String,String>();
        for (Element textElement : getSubElements(container, elementName)) {
            String language = textElement.getAttribute("language"); // empty string if no langage="x" defined
            if (language.equals("")) {
                if (defaultLang != null)
                    throw new ConfigurationException("Multiple <" + elementName + "> without language attribute found");
                defaultLang = textElement.getTextContent();
            } else {
                if ( ! language.matches("[a-z]{2}")) throw new ConfigurationException("<" + elementName +
                    " language='" + language + "'> found, yet language must be 2 letters");
                if (nonDefaultLang.containsKey(language)) throw new ConfigurationException("<" + elementName +
                    " language='" + language + "'> found multiple times");
                nonDefaultLang.put(language, textElement.getTextContent());
            }
        }
        if (nonDefaultLang.size() > 0 && defaultLang == null) throw new ConfigurationException("<" + elementName +
            "> is missing the default entry without 'language' attribute");

        if (defaultLang != null) return new LocalizableText(defaultLang, nonDefaultLang);
        else return null;
    }

    protected static LocalizableText parseMandatoryLocalizableText(Element container, String elementName)
    throws ConfigurationException {
        LocalizableText result = parseOptionalLocalizableText(container, elementName);
        if (result == null) throw new ConfigurationException("<" + elementName + "> is mandatory");
        return result;
    }

    protected static LocalizableText parseOptionalLocalizableTextOrDefault(Element container, String elementName)
    throws ConfigurationException {
        LocalizableText result = parseOptionalLocalizableText(container, elementName);
        if (result == null) return new LocalizableText("", new HashMap<String,String>());
        return result;
    }

    /** @return null if no &lt;img> tag found */
    protected static LocalizableImage parseOptionalLocalizableImage(Element container)
    throws ConfigurationException {
        Map<String, LocalizableImage.Img> map = new HashMap<String, LocalizableImage.Img>();
        for (Element imgElement : getSubElements(container, "img")) {
            String language = imgElement.getAttribute("language"); // empty string if no langage="x" defined
            if ( ! language.equals("") && ! language.matches("[a-z]{2}")) throw new ConfigurationException("<img language='" +
                language + "'> found, yet language must be 2 letters");
            if (map.containsKey(language)) throw new ConfigurationException("<img language='"+ language +"'> found multiple times");

            LocalizableImage.Img i = new LocalizableImage.Img();
            i.url = imgElement.getAttribute("src");
            if (i.url.equals("")) throw new ConfigurationException("<img language='" + language + "> missing 'src' attribute");
            try {
                i.width  = imgElement.getAttribute("width") .equals("") ? 0 : Integer.parseInt(imgElement.getAttribute("width"));
                i.height = imgElement.getAttribute("height").equals("") ? 0 : Integer.parseInt(imgElement.getAttribute("height"));
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException("<img language='"+ language +"'>: width or height are not numbers");
            }

            map.put(language, i);
        }
        if (map.size() > 0 && ! map.containsKey(""))
            throw new ConfigurationException("<img> is missing the default entry without 'language' attribute");

        if (map.size() > 0) return new LocalizableImage(map);
        else return null;
    }

    /** @return a PrivilegeRestriction allowing all users, if tag is missing */
    protected static PrivilegeRestriction parsePrivilegeRestriction(Element container) throws ConfigurationException {
        List<Element> n = getSubElements(container, "privilege-restriction");
        if (n.size() > 1) throw new ConfigurationException("Too many <privilege-restriction> elements");
        if (n.isEmpty()) return PrivilegeRestriction.allowAny();
        Element p = n.get(0);

        PrivilegeRestriction.Type type;
        if ("all-but".equals(p.getAttribute("type"))) type = PrivilegeRestriction.Type.AllBut;
        else if ("none-but".equals(p.getAttribute("type"))) type = PrivilegeRestriction.Type.NoneBut;
        else throw new ConfigurationException("<privilege-restriction>: didn't understand type='" + p.getAttribute("type") + "'");

        Set<Privilege> privileges = new HashSet<Privilege>();
        for (Element pp : getSubElements(p, "privilege")) {
            String privName = pp.getAttribute("name");
            if (privName.equals("")) throw new ConfigurationException("Each <privilege> must have a name attribute");
            privileges.add(new Privilege(privName));
        }

        return new PrivilegeRestriction(type, privileges);
    }
}
