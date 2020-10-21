package com.yogafire.sql.changer;

import com.yogafire.sql.changer.formatter.base.SqlType;

import static com.yogafire.sql.changer.BaseTests.*;

/**
 * Created by yogafire on 2020/10/21
 */
public class ArrayFunctionTests {

    public static void main(String[] args) {
        testArrayIndex();
        testCreateArray();
    }

    public static void testArrayIndex() {
        String sql = "select a[cardinality(a)] from (select array[1,2,3] as a)";
        println(SqlFormatter.change(sql, SqlType.HIVE_SQL));
        println(SqlFormatter.change(sql, SqlType.PRESTO_SQL));
    }

    public static void testCreateArray() {
        String sql = "select array(1,2,3)";
        println(SqlFormatter.change("sql", SqlType.HIVE_SQL));
        println(SqlFormatter.change("sql", SqlType.PRESTO_SQL));
    }

}
