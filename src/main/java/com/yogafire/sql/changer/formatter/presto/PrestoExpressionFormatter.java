package com.yogafire.sql.changer.formatter.presto;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.yogafire.sql.changer.exception.UnsupportedSqlException;
import com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter;
import com.yogafire.sql.changer.tree.*;

import java.util.List;
import java.util.Optional;

import static com.yogafire.sql.changer.SqlFormatter.formatSql;

/**
 * Created by yogafire on 2020/7/23
 */
public class PrestoExpressionFormatter extends BaseExpressionFormatter {
    public PrestoExpressionFormatter(Optional<List<Expression>> parameters) {
        super(parameters);
    }

    @Override
    protected String visitSubscriptExpression(SubscriptExpression node, Void context) {
        return formatSql(node.getBase(), parameters) + "[" + formatSql(node.getIndex(), parameters) + " + 1]";
    }

    @Override
    protected String visitArrayConstructor(ArrayConstructor node, Void context) {
        ImmutableList.Builder<String> valueStrings = ImmutableList.builder();
        for (Expression value : node.getValues()) {
            valueStrings.add(formatSql(value, parameters));
        }
        return "ARRAY[" + Joiner.on(",").join(valueStrings.build()) + "]";
    }

    @Override
    protected String visitLikePredicate(LikePredicate node, Void context) {
        if (node.getType() == LikePredicate.LikeType.RLIKE) {
            throw new UnsupportedSqlException("[presto] Not supported 'RLIKE'");
        }
        return super.visitLikePredicate(node, context);
    }

    @Override
    protected String visitArithmeticBinary(ArithmeticBinaryExpression node, Void context) {
        ArithmeticBinaryExpression.Operator operator = node.getOperator();
        if (operator == ArithmeticBinaryExpression.Operator.BIT_AND ||
                operator == ArithmeticBinaryExpression.Operator.BIT_OR ||
                operator == ArithmeticBinaryExpression.Operator.BIT_NOT ||
                operator == ArithmeticBinaryExpression.Operator.BIT_XOR ||
                operator == ArithmeticBinaryExpression.Operator.BIT_L_SHIFT ||
                operator == ArithmeticBinaryExpression.Operator.BIT_R_SHIFT) {
            throw new UnsupportedSqlException("[presto] Not supported bit arithmetic '&','|','!','^','<<','>>'");
        }
        return super.visitArithmeticBinary(node, context);
    }

    @Override
    protected String visitArithmeticUnary(ArithmeticUnaryExpression node, Void context) {
        if (node.getSign() == ArithmeticUnaryExpression.Sign.BIT_NOT) {
            throw new UnsupportedSqlException("[presto] Not supported bit arithmetic '!'");
        }
        return super.visitArithmeticUnary(node, context);
    }

    @Override
    public void functionConvert(StringBuilder builder, FunctionCall node) {
        String name = node.getName().toString();
        if ("from_unixtime".equals(name) && node.getArguments().size() == 2) {
            Expression time = node.getArguments().get(0);
            Expression format = node.getArguments().get(0);
            builder.append("format_detatime(")
                    .append(name)
                    .append("(")
                    .append(process(time))
                    .append("), ")
                    .append(process(format))
                    .append(")");
        } else {
            super.functionConvert(builder, node);
        }
    }

    @Override
    public String realFunctionName(String name) {
        switch (name) {
            case "regexp":
                return "regexp_like";
            case "get_json_object":
                return "json_extract_scalar";
            default:
                return name;
        }
    }
}
