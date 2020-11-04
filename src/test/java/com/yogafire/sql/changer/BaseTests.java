package com.yogafire.sql.changer;

import com.yogafire.sql.changer.formatter.base.SqlType;

/**
 * Created by yogafire on 2020/10/21
 */
public class BaseTests {

    public static void println(String text) {
        System.out.println(text);
    }

    public static void change(String sql) {
        for (SqlType type : SqlType.values()) {
            try {
                println(type.name() + ": " + SqlFormatter.change(sql, type));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
