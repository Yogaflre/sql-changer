package com.yogafire.sql.changer.formatter;

import com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter;
import com.yogafire.sql.changer.tree.Expression;
import com.yogafire.sql.changer.tree.LongLiteral;
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
        Expression prestoIndex = node.getIndex();
        if (node.getIndex() instanceof LongLiteral) {
            LongLiteral index = (LongLiteral) node.getIndex();
            prestoIndex = new LongLiteral(index.getLocation(), String.valueOf(index.getValue() + 1));
        }
        return formatSql(node.getBase(), parameters) + "[" + formatSql(prestoIndex, parameters) + "]";
    }

}
