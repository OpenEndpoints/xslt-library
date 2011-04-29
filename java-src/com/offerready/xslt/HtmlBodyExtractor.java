package com.offerready.xslt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.databasesandlife.util.Timer;

/**
 * Extracts the body of an HTML report document.
 *    <p>
 * An "HTML document generator" generates HTML, suitable for viewing in a browser, or inclusion in an iframe. 
 * However this HTML contains e.g. &lt;html&gt; tags which are not suitable for display within a &lt;div&gt;.
 * Objects of this class extract the relevant information from the HTML document, and return an HTML
 * string suitable for inclusion in a &lt;div&gt;.
 *    <p>
 * The entire contents of the &lt;body&gt; are returned.
 * Any &lt;style&gt; tags in the &lt;head&gt; are extracted are prepended to this body.
 */
public class HtmlBodyExtractor {
    
    protected List<String> scriptsToIgnore = new ArrayList<String>();
    
    class Range {
        int startIncl, endExcl;
        Range(int s, int e) { startIncl = s; endExcl = e; }
        boolean overlaps(Range x) { 
            if (x.endExcl <= startIncl) return false;
            if (x.startIncl >= endExcl) return false;
            return true;
        }
    }
    
    /**
     * For example if JQuery should not be included; simply add "jquery" to this method and this &lt;script&gt;
     * tag will not be included in the result.
     */
    public HtmlBodyExtractor addScriptToIgnore(String scriptSubstring) {
        scriptsToIgnore.add(scriptSubstring);
        return this;
    }
    
    protected String ignoreScripts(String html) {
        for (String s : scriptsToIgnore)
            html = html.replaceAll(
                "<script src=['\"][^'\"]*" + Pattern.quote(s) + "[^'\"]*['\"]></script>", 
                "<!-- ignoring " + s + " -->");
        return html;
    }
    
    protected void extractElements(
        StringBuilder result, String input, 
        List<Range> ranges, 
        String start, String endOfStartOrNull, String end
    ) {
        int startIdx = -1;
        StringBuffer ourResult = new StringBuffer();
        while ((startIdx = input.indexOf(start, startIdx+1)) >= 0) {
            int idxOfEndTag = input.indexOf(end, startIdx);
            Range range = endOfStartOrNull == null 
                ? new Range(startIdx, idxOfEndTag + end.length())
                : new Range(input.indexOf(endOfStartOrNull, startIdx) + endOfStartOrNull.length(), idxOfEndTag);
            boolean rangeOverlaps = false;
            for (Range r : ranges) if (r.overlaps(range)) rangeOverlaps = true;
            if (rangeOverlaps) continue;
            ourResult.append(ignoreScripts(input.substring(range.startIncl, range.endExcl)));
            ranges.add(range);
        }
        ourResult.append("\n");
        result.insert(0, ourResult);
    }
    
    public String extractBody(String htmlText) {
        try (Timer t = new Timer("HtmlBodyExtractor.extractBody")) {
            StringBuilder result  = new StringBuilder();  // 2.5 seconds at 1k iterations on i3
            List<Range> ranges = new ArrayList<Range>();
            
            extractElements(result, htmlText, ranges, "<body",         ">",  "</body>");
            extractElements(result, htmlText, ranges, "<!--[if IE]>",  null, "<![endif]-->");
            extractElements(result, htmlText, ranges, "<!--[if !IE]>", null, "<!--<![endif]-->");
            extractElements(result, htmlText, ranges, "<style",        null, "</style>");
                
            return result.toString();
        }
    }
}
