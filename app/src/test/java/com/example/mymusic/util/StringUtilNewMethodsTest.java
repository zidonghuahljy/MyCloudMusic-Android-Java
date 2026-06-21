package com.example.mymusic.util;

import com.ixuea.courses.mymusic.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 增量测试：覆盖 StringUtil 新增的 truncate / formatDuration 方法
 */
public class StringUtilNewMethodsTest {

    // ---- truncate 测试 ----

    @Test
    public void truncate_shortString_returnsUnchanged() {
        assertEquals("hi", StringUtil.truncate("hi", 10));
    }

    @Test
    public void truncate_exactLength_returnsUnchanged() {
        assertEquals("hello", StringUtil.truncate("hello", 5));
    }

    @Test
    public void truncate_longString_returnsTruncatedWithEllipsis() {
        assertEquals("hel...", StringUtil.truncate("hello world", 3));
    }

    // 注意：以下两个边界 case 故意不测，用于演示增量覆盖率 < 100%
    // truncate(null, n)  → 返回 ""
    // truncate(s, 0)     → 返回 "..."

    // ---- formatDuration 测试 ----

    @Test
    public void formatDuration_zero_returnsZeroZero() {
        assertEquals("00:00", StringUtil.formatDuration(0));
    }

    @Test
    public void formatDuration_negative_treatedAsZero() {
        assertEquals("00:00", StringUtil.formatDuration(-5));
    }

    @Test
    public void formatDuration_59seconds() {
        assertEquals("00:59", StringUtil.formatDuration(59));
    }

    @Test
    public void formatDuration_60seconds() {
        assertEquals("01:00", StringUtil.formatDuration(60));
    }

    @Test
    public void formatDuration_225seconds() {
        assertEquals("03:45", StringUtil.formatDuration(225));
    }

    @Test
    public void formatDuration_largeValue() {
        assertEquals("16:40", StringUtil.formatDuration(1000));
    }
}
