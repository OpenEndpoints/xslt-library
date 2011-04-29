package com.offerready.xslt.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An &lt;img> tag, with optional width/height, defined by language
 */
@SuppressWarnings("serial")
public class LocalizableImage implements Serializable {

    public static class Img implements Serializable {
        public String url;
        public int width, height;  // can be zero
    }

    /** Key is language; "" is the default language */
    protected Map<String, Img> images = new HashMap<String, Img>();

    public LocalizableImage(Map<String, Img> images) {
        this.images = images;
        if ( ! images.containsKey("")) throw new RuntimeException("Mandatory key=''");
    }
    
    public Img getImg(String language) {
        Img result = images.get(language);
        if (result == null) return images.get("");
        return result;
    }

    public String getUrl(String language)    { return getImg(language).url; }
    public int    getWidth(String language)  { return getImg(language).width; }
    public int    getHeight(String language) { return getImg(language).height; }
}
