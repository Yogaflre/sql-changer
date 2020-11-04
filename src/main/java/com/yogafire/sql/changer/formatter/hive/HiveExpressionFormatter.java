package com.yogafire.sql.changer.formatter.hive;

import com.yogafire.sql.changer.exception.UnsupportedSqlException;
import com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter;
import com.yogafire.sql.changer.tree.*;

import java.util.List;
import java.util.Optional;

import static com.yogafire.sql.changer.SqlFormatter.formatSql;

/**
 * Created by yogafire on 2020/7/24
 */
public class HiveExpressionFormatter extends BaseExpressionFormatter {

    public HiveExpressionFormatter(Optional<List<Expression>> parameters) {
        super(parameters);
    }

    @Override
    protected String visitSubscriptExpression(SubscriptExpression node, Void context) {
        // TODO 过滤掉hive不支持的下标类型
        return formatSql(node.getBase(), parameters) + "[" + formatSql(node.getIndex(), parameters) + "]";
    }

    @Override
    protected String visitArithmeticBinary(ArithmeticBinaryExpression node, Void context) {
        ArithmeticBinaryExpression.Operator operator = node.getOperator();
        if (operator == ArithmeticBinaryExpression.Operator.BIT_L_SHIFT ||
                operator == ArithmeticBinaryExpression.Operator.BIT_R_SHIFT) {
            throw new UnsupportedSqlException("[hive] Not supported bit arithmetic '<<' , '>>'");
        }
        return super.visitArithmeticBinary(node, context);
    }

    @Override
    protected String visitArithmeticUnary(ArithmeticUnaryExpression node, Void context) {
        String value = process(node.getValue(), context);

        switch (node.getSign()) {
            case MINUS:
                // this is to avoid turning a sequence of "-" into a comment (i.e., "-- comment")
                String separator = value.startsWith("-") ? " " : "";
                return "-" + separator + value;
            case PLUS:
                return "+" + value;
            case BIT_NOT:
                return "~" + value;
            default:
                throw new UnsupportedOperationException("Unsupported sign: " + node.getSign());
        }
    }


    @Override
    public void functionConvert(StringBuilder builder, FunctionCall node) {
        String name = node.getName().toString();
        if ("format_datetime".equals(name) && node.getArguments().size() == 2) {
            Expression time = node.getArguments().get(0);
            Expression format = node.getArguments().get(1);
            if (time instanceof FunctionCall) {
                FunctionCall timeFunc = (FunctionCall) time;
                if ("from_unixtime".equals(timeFunc.getName().toString()) && timeFunc.getArguments().size() == 1) {
                    builder.append("from_unixtime(")
                            .append(process(timeFunc.getArguments().get(0)))
                            .append(", ")
                            .append(process(format))
                            .append(")");
                    return;
                }
            }
            throw new UnsupportedSqlException("[hive] Not supported function 'format_datetime'");
        } else {
            super.functionConvert(builder, node);
        }
    }

    @Override
    public String realFunctionName(String name) {
        switch (name) {
            case "regexp_like":
                return "regexp";
            case "json_extract_scalar":
                return "get_json_object";
            default:
                return name;
        }
    }
}
