package com.yogafire.sql.changer;

import com.yogafire.sql.changer.formatter.base.SqlType;

public class ChangerTest {

    public static void main(String[] args) {
        String sql = "select a[cardinality(a)] from (select array[1,2,3] as a)";
        prestoTest(sql);
        hiveTest(sql);
    }

    public static void prestoTest(String sql) {
        String s = SqlFormatter.change(sql, SqlType.PRESTO_SQL);
        System.out.println(s);
    }

    public static void hiveTest(String sql) {
        String s = SqlFormatter.change(sql, SqlType.HIVE_SQL);
        System.out.println(s);
    }
}
