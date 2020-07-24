package com.yogafire.sql.changer.formatter;

import com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter;
import com.yogafire.sql.changer.tree.Expression;
import com.yogafire.sql.changer.tree.LongLiteral;
import com.yogafire.sql.changer.tree.SubscriptExpression;

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
        if (!(node.getIndex() instanceof LongLiteral)) {
            throw new UnsupportedOperationException("Hive subscript expression only support number");
        }
        return formatSql(node.getBase(), parameters) + "[" + formatSql(node.getIndex(), parameters) + "]";
    }
}
