package com.yogafire.sql.changer.formatter.presto;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter;
import com.yogafire.sql.changer.tree.ArrayConstructor;
import com.yogafire.sql.changer.tree.Expression;
import com.yogafire.sql.changer.tree.SubscriptExpression;

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

}
