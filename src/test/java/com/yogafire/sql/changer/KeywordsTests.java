package com.yogafire.sql.changer;

import static com.yogafire.sql.changer.BaseTests.change;

/**
 * 关键字
 * Created by yogafire on 2020/10/21
 */
public class KeywordsTests {

    public static void main(String[] args) {
        testRlike();
        testBit();
    }

    private static void testRlike() {
        change("select 'footbar' RLIKE '^f.*r$'");
    }

    private static void testBit() {
        change("select 1 & 2");
        change("select 1 | 2");
        change("select 1 ^ 2");
        change("select 3 >> 1");
        change("select 3 << 1");
        change("select !1");
    }

}
