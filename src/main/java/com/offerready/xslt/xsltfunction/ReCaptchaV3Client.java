package com.offerready.xslt.xsltfunction;

import com.databasesandlife.util.PlaintextParameterReplacer;
import com.databasesandlife.util.Timer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class ReCaptchaV3Client {
    
    protected final static String urlPattern =
        "https://www.google.com/recaptcha/api/siteverify?secret=${serverSideKey}&response=${tokenFromRequest}";

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class Response {
        public boolean success;
        @JsonProperty("error-codes") public @CheckForNull List<String> errorCodes;
        public double score;
    }

    public static double check(String serverSideKey, String tokenFromRequest) {
        try (var ignored = new Timer("ReCaptchaV3Client.check")) {
            var params = new HashMap<String, String>();
            params.put("serverSideKey", serverSideKey);
            params.put("tokenFromRequest", tokenFromRequest);
            var urlExpanded = PlaintextParameterReplacer.replacePlainTextParameters(urlPattern, params);
            var response = new ObjectMapper().readValue(new URL(urlExpanded), Response.class);
            if ( ! response.success) 
                throw new RuntimeException(String.format("Response JSON contains: success=false; errorCodes=[%s]", 
                    String.join(",", Optional.ofNullable(response.errorCodes).orElse(emptyList()))));
            return response.score;
        }
        catch (Exception e) {
            Logger.getLogger(ReCaptchaV3Client.class).warn(
                "Exception occurred during ReCaptcha processing: returning -1 to XSLT", e);
            return -1;
        }
    }
}
