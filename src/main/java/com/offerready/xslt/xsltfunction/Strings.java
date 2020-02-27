package com.offerready.xslt.xsltfunction;

import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.util.HashSet;

public class Strings {
    
    public static @Nonnull String randomLowercaseLetter() {
        val chars = new HashSet<Character>();
        for (char c = 'a'; c <= 'z'; c++) chars.add(c);

        val result = new StringBuilder();
        for (char c : chars) result.append(c);

        return RandomStringUtils.random(1, result.toString());
    }
}
