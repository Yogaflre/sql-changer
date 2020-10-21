package com.yogafire.sql.changer.formatter.hive;

import com.google.common.base.Joiner;
import com.yogafire.sql.changer.formatter.base.BaseSqlFormatter;
import com.yogafire.sql.changer.tree.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter.*;

/**
 * Created by yogafire on 2020/7/22
 */
public class HiveSqlFormatter extends BaseSqlFormatter {

    public HiveSqlFormatter(StringBuilder builder, Optional<List<Expression>> parameters) {
        super(builder, parameters);
    }

    @Override
    public String formatExpression(Expression expression, Optional<List<Expression>> parameters) {
        return new HiveExpressionFormatter(parameters).process(expression, null);
    }

    @Override
    protected Void visitQuerySpecification(QuerySpecification node, Integer indent) {
        process(node.getSelect(), indent);

        if (node.getFrom().isPresent()) {
            append(indent, "FROM");
            builder.append('\n');
            append(indent, "  ");
            process(node.getFrom().get(), indent);
        }

        builder.append('\n');

        if (node.getWhere().isPresent()) {
            append(indent, "WHERE " + formatExpression(node.getWhere().get(), parameters))
                    .append('\n');
        }

        if (node.getGroupBy().isPresent()) {
            append(indent, "GROUP BY " + (node.getGroupBy().get().isDistinct() ? " DISTINCT " : "") + formatGroupBy(node.getGroupBy().get().getGroupingElements())).append('\n');
        }

        if (node.getHaving().isPresent()) {
            append(indent, "HAVING " + formatExpression(node.getHaving().get(), parameters))
                    .append('\n');
        }

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), indent);
        }

        if (node.getClusterBy().isPresent()) {
            process(node.getClusterBy().get(), indent);
        } else {
            if (node.getDistributeBy().isPresent()) {
                process(node.getDistributeBy().get(), indent);
            }
            if (node.getSortBy().isPresent()) {
                process(node.getSortBy().get(), indent);
            }
        }

        if (node.getLimit().isPresent()) {
            append(indent, "LIMIT " + node.getLimit().get())
                    .append('\n');
        }
        return null;
    }


    @Override
    protected Void visitInsert(Insert node, Integer indent) {
        builder.append("INSERT ")
                .append(node.getType()).append(" ")
                .append(node.getTarget());
        if (node.getColumns().isPresent()) {
            builder.append(" (")
                    .append(Joiner.on(", ").join(node.getColumns().get()))
                    .append(")");
        }
        builder.append("\n");
        process(node.getQuery(), indent);
        return null;
    }

    @Override
    protected Void visitClusterBy(ClusterBy node, Integer indent) {
        append(indent, "CLUSTER BY ")
                .append(Joiner.on(", ").join(node.getExpression().stream().map(e -> formatExpression(e, parameters)).collect(Collectors.toList())))
                .append("\n");
        return null;
    }

    @Override
    protected Void visitDistributeBy(DistributeBy node, Integer indent) {
        append(indent, "DISTRIBUTE BY ")
                .append(Joiner.on(", ").join(node.getExpression().stream().map(e -> formatExpression(e, parameters)).collect(Collectors.toList())))
                .append("\n");
        return null;
    }

    @Override
    protected Void visitSortBy(SortBy node, Integer indent) {
        append(indent, "SORT BY " + formatSortItems(node.getSortItems(), parameters))
                .append('\n');
        return null;
    }
}
