package com.yogafire.sql.changer.formatter.presto;

import com.yogafire.sql.changer.formatter.base.BaseSqlFormatter;
import com.yogafire.sql.changer.tree.Expression;

import java.util.List;
import java.util.Optional;

/**
 * Created by yogafire on 2020/7/22
 */
public class PrestoSqlFormatter extends BaseSqlFormatter {

    public PrestoSqlFormatter(StringBuilder builder, Optional<List<Expression>> parameters) {
        super(builder, parameters);
    }

    @Override
    public String formatExpression(Expression expression, Optional<List<Expression>> parameters) {
        return new PrestoExpressionFormatter(parameters).process(expression, null);
    }

}
