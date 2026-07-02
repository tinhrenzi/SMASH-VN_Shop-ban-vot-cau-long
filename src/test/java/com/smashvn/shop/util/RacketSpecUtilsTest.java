package com.smashvn.shop.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RacketSpecUtilsTest {

    @Test
    void testFormatTensionRange_SuccessLbs() {
        String result = RacketSpecUtils.formatTensionRange("20", "28");
        assertEquals("20 - 28 lbs", result);
    }

    @Test
    void testFormatTensionRange_SuccessKgToLbs() {
        String result = RacketSpecUtils.formatTensionRange("9 kg", "12.5 kg");
        assertEquals("19.8 - 27.6 lbs", result);
    }

    @Test
    void testFormatTensionRange_MinGreaterThanMax_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            RacketSpecUtils.formatTensionRange("28", "20");
        });
    }

    @Test
    void testFormatTensionRange_InvalidTension_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            RacketSpecUtils.formatTensionRange("abc", "28");
        });
    }
}
