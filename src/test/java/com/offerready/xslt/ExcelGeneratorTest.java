package com.offerready.xslt;

import junit.framework.TestCase;

import static com.offerready.xslt.ExcelGenerator.InputDecimalSeparator.magic;

public class ExcelGeneratorTest extends TestCase {
    
    public void test_magic_tryParseNumber() throws Exception {
        assertEquals(1000.5, magic.tryParseNumber("1,000.50"));
        assertEquals(1000.5, magic.tryParseNumber("1.000,50"));
        assertEquals(1000.5, magic.tryParseNumber("1'000.50"));

        assertEquals(1.5, magic.tryParseNumber("1.50"));
        assertEquals(1.5, magic.tryParseNumber("1,50"));
        assertEquals(1.5, magic.tryParseNumber("1.50"));

        assertEquals(1000.0, magic.tryParseNumber("1,000"));
        assertEquals(1000.0, magic.tryParseNumber("1.000"));
        assertEquals(1000.0, magic.tryParseNumber("1'000"));

        assertEquals(1.0, magic.tryParseNumber("1"));
        
        assertNull(magic.tryParseNumber("foo"));
    }
}