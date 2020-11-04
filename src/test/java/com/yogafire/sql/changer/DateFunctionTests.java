package com.yogafire.sql.changer;

import static com.yogafire.sql.changer.BaseTests.change;

/**
 * 日期函数
 * Created by yogafire on 2020/10/27
 */
public class DateFunctionTests {

    public static void main(String[] args) {
        formatDate();
    }

    private static void formatDate() {
        change("select from_unixtime(1323308943,'yyyyMMdd')");
        change("select format_datetime(from_unixtime(1323308943),'yyyyMMdd')");
    }

}
