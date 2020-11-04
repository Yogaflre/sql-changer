package com.yogafire.sql.changer;

import static com.yogafire.sql.changer.BaseTests.change;

/**
 * 数组函数
 * Created by yogafire on 2020/10/21
 */
public class ArrayFunctionTests {

    public static void main(String[] args) {
        testArrayIndex();
        testCreateArray();
    }

    private static void testArrayIndex() {
        change("select array(1,2,3)[0]");
    }

    private static void testCreateArray() {
        change("select array(1,2,3)");
    }

}
