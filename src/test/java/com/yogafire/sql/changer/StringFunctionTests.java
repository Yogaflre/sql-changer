package com.yogafire.sql.changer;

import static com.yogafire.sql.changer.BaseTests.change;

/**
 * 字符串函数
 * Created by yogafire on 2020/10/21
 */
public class StringFunctionTests {

    public static void main(String[] args) {
//        regexp();
//        json();
    }

    private static void regexp() {
        change("SELECT regexp_like('1a 2b 14m', '\\d+b')");
        change("SELECT regexp('1a 2b 14m', '\\d+b')");
    }

    private static void json() {
        change("select json_extract_scalar('{\"a\":\"1\"}','$.a')");
        change("select get_json_object('{\"a\":\"1\"}','$.a')");
    }
}
