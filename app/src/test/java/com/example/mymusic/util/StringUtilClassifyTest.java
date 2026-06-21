package com.example.mymusic.util;

import com.ixuea.courses.mymusic.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilClassifyTest {

    @Test
    public void classify_scoreBelow70_returnsC() {
        assertEquals("C", StringUtil.classify(50));
        assertEquals("C", StringUtil.classify(0));
        assertEquals("C", StringUtil.classify(69));
    }

    @Test
    public void classify_scoreBetween70And89_returnsB() {
        assertEquals("B", StringUtil.classify(70));
        assertEquals("B", StringUtil.classify(75));
        assertEquals("B", StringUtil.classify(89));
    }

    // 故意不测 score >= 90，让 return "A" 那行保持红色（uncovered）
}
