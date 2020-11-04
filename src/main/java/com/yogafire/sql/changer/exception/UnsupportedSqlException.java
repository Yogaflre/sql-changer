package com.yogafire.sql.changer.exception;

/**
 * 不支持SQL异常
 * Created by yogafire on 2020/10/27
 */
public class UnsupportedSqlException extends RuntimeException {
    public UnsupportedSqlException(String message) {
        super(message);
    }
}
