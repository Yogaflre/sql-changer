package com.yogafire.sql.changer.formatter.base;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.yogafire.sql.changer.exception.UnsupportedSqlException;
import com.yogafire.sql.changer.tree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.yogafire.sql.changer.formatter.base.BaseExpressionFormatter.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Created by yogafire on 2020/7/22
 */
public class BaseSqlFormatter extends AstVisitor<Void, Integer> {
    private static final String INDENT = "   ";
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    public final StringBuilder builder;
    public final Optional<List<Expression>> parameters;

    public BaseSqlFormatter(StringBuilder builder, Optional<List<Expression>> parameters) {
        this.builder = builder;
        this.parameters = parameters;
    }

    public String formatExpression(Expression expression, Optional<List<Expression>> parameters) {
        return new BaseExpressionFormatter(parameters).process(expression, null);
    }

    @Override
    protected Void visitNode(Node node, Integer indent) {
        throw new UnsupportedOperationException("not yet implemented: " + node);
    }

    @Override
    protected Void visitExpression(Expression node, Integer indent) {
        checkArgument(indent == 0, "visitExpression should only be called at root");
        builder.append(formatExpression(node, parameters));
        return null;
    }

    @Override
    protected Void visitUnnest(Unnest node, Integer indent) {
        builder.append("UNNEST(")
                .append(node.getExpressions().stream()
                        .map(expression -> formatExpression(expression, parameters))
                        .collect(joining(", ")))
                .append(")");
        if (node.isWithOrdinality()) {
            builder.append(" WITH ORDINALITY");
        }
        return null;
    }

    @Override
    protected Void visitLateral(Lateral node, Integer indent) {
        append(indent, "LATERAL (");
        process(node.getQuery(), indent + 1);
        append(indent, ")");
        return null;
    }

    @Override
    protected Void visitPrepare(Prepare node, Integer indent) {
        append(indent, "PREPARE ");
        builder.append(node.getName());
        builder.append(" FROM");
        builder.append("\n");
        process(node.getStatement(), indent + 1);
        return null;
    }

    @Override
    protected Void visitDeallocate(Deallocate node, Integer indent) {
        append(indent, "DEALLOCATE PREPARE ");
        builder.append(node.getName());
        return null;
    }

    @Override
    protected Void visitExecute(Execute node, Integer indent) {
        append(indent, "EXECUTE ");
        builder.append(node.getName());
        List<Expression> parameters = node.getParameters();
        if (!parameters.isEmpty()) {
            builder.append(" USING ");
            Joiner.on(", ").appendTo(builder, parameters);
        }
        return null;
    }

    @Override
    protected Void visitDescribeOutput(DescribeOutput node, Integer indent) {
        append(indent, "DESCRIBE OUTPUT ");
        builder.append(node.getName());
        return null;
    }

    @Override
    protected Void visitDescribeInput(DescribeInput node, Integer indent) {
        append(indent, "DESCRIBE INPUT ");
        builder.append(node.getName());
        return null;
    }

    @Override
    protected Void visitQuery(Query node, Integer indent) {
        if (node.getWith().isPresent()) {
            With with = node.getWith().get();
            append(indent, "WITH");
            if (with.isRecursive()) {
                builder.append(" RECURSIVE");
            }
            builder.append("\n  ");
            Iterator<WithQuery> queries = with.getQueries().iterator();
            while (queries.hasNext()) {
                WithQuery query = queries.next();
                append(indent, formatExpression(query.getName(), parameters));
                query.getColumnNames().ifPresent(columnNames -> appendAliasColumns(builder, columnNames));
                builder.append(" AS ");
                process(new TableSubquery(query.getQuery()), indent);
                builder.append('\n');
                if (queries.hasNext()) {
                    builder.append(", ");
                }
            }
        }

        processRelation(node.getQueryBody(), indent);

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), indent);
        }

        if (node.getLimit().isPresent()) {
            append(indent, "LIMIT " + node.getLimit().get())
                    .append('\n');
        }

        return null;
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

        if (node.getClusterBy().isPresent() || node.getDistributeBy().isPresent() || node.getSortBy().isPresent()) {
            throw new UnsupportedOperationException("'CLUSTER BY/DISTRIBUTE BY/SORT BY' not supported in base com.yogafire.sql.changer.formatter");
        }

        if (node.getLimit().isPresent()) {
            append(indent, "LIMIT " + node.getLimit().get())
                    .append('\n');
        }
        return null;
    }

    @Override
    protected Void visitOrderBy(OrderBy node, Integer indent) {
        append(indent, formatOrderBy(node, parameters))
                .append('\n');
        return null;
    }

    @Override
    protected Void visitSelect(Select node, Integer indent) {
        append(indent, "SELECT");
        if (node.isDistinct()) {
            builder.append(" DISTINCT");
        }

        if (node.getSelectItems().size() > 1) {
            boolean first = true;
            for (SelectItem item : node.getSelectItems()) {
                builder.append("\n")
                        .append(indentString(indent))
                        .append(first ? "  " : ", ");

                process(item, indent);
                first = false;
            }
        } else {
            builder.append(' ');
            process(getOnlyElement(node.getSelectItems()), indent);
        }

        builder.append('\n');

        return null;
    }

    @Override
    protected Void visitSingleColumn(SingleColumn node, Integer indent) {
        builder.append(formatExpression(node.getExpression(), parameters));
        if (node.getAlias().isPresent()) {
            builder.append(' ')
                    .append(formatExpression(node.getAlias().get(), parameters));
        }

        return null;
    }

    @Override
    protected Void visitAllColumns(AllColumns node, Integer context) {
        builder.append(node.toString());

        return null;
    }

    @Override
    protected Void visitTable(Table node, Integer indent) {
        builder.append(formatName(node.getName()));

        return null;
    }

    @Override
    protected Void visitJoin(Join node, Integer indent) {
        JoinCriteria criteria = node.getCriteria().orElse(null);
        String type = node.getType().toString();
        if (criteria instanceof NaturalJoin) {
            type = "NATURAL " + type;
        }

        if (node.getType() != Join.Type.IMPLICIT) {
            builder.append('(');
        }
        process(node.getLeft(), indent);

        builder.append('\n');
        if (node.getType() == Join.Type.IMPLICIT) {
            append(indent, ", ");
        } else {
            append(indent, type).append(" JOIN ");
        }

        process(node.getRight(), indent);

        if (node.getType() != Join.Type.CROSS && node.getType() != Join.Type.IMPLICIT) {
            if (criteria instanceof JoinUsing) {
                JoinUsing using = (JoinUsing) criteria;
                builder.append(" USING (")
                        .append(Joiner.on(", ").join(using.getColumns()))
                        .append(")");
            } else if (criteria instanceof JoinOn) {
                JoinOn on = (JoinOn) criteria;
                builder.append(" ON ")
                        .append(formatExpression(on.getExpression(), parameters));
            } else if (!(criteria instanceof NaturalJoin)) {
                throw new UnsupportedOperationException("unknown join criteria: " + criteria);
            }
        }

        if (node.getType() != Join.Type.IMPLICIT) {
            builder.append(")");
        }

        return null;
    }

    @Override
    protected Void visitAliasedRelation(AliasedRelation node, Integer indent) {
        process(node.getRelation(), indent);

        builder.append(' ')
                .append(formatExpression(node.getAlias(), parameters));
        appendAliasColumns(builder, node.getColumnNames());

        return null;
    }

    @Override
    protected Void visitSampledRelation(SampledRelation node, Integer indent) {
        process(node.getRelation(), indent);

        builder.append(" TABLESAMPLE ")
                .append(node.getType())
                .append(" (")
                .append(node.getSamplePercentage())
                .append(')');

        return null;
    }

    @Override
    protected Void visitValues(Values node, Integer indent) {
        builder.append(" VALUES ");

        boolean first = true;
        for (Expression row : node.getRows()) {
            builder.append("\n")
                    .append(indentString(indent))
                    .append(first ? "  " : ", ");

            builder.append(formatExpression(row, parameters));
            first = false;
        }
        builder.append('\n');

        return null;
    }

    @Override
    protected Void visitTableSubquery(TableSubquery node, Integer indent) {
        builder.append('(')
                .append('\n');

        process(node.getQuery(), indent + 1);

        append(indent, ") ");

        return null;
    }

    @Override
    protected Void visitUnion(Union node, Integer indent) {
        Iterator<Relation> relations = node.getRelations().iterator();

        while (relations.hasNext()) {
            processRelation(relations.next(), indent);

            if (relations.hasNext()) {
                builder.append("UNION ");
                if (!node.isDistinct()) {
                    builder.append("ALL ");
                }
            }
        }

        return null;
    }

    @Override
    protected Void visitExcept(Except node, Integer indent) {
        processRelation(node.getLeft(), indent);

        builder.append("EXCEPT ");
        if (!node.isDistinct()) {
            builder.append("ALL ");
        }

        processRelation(node.getRight(), indent);

        return null;
    }

    @Override
    protected Void visitIntersect(Intersect node, Integer indent) {
        Iterator<Relation> relations = node.getRelations().iterator();

        while (relations.hasNext()) {
            processRelation(relations.next(), indent);

            if (relations.hasNext()) {
                builder.append("INTERSECT ");
                if (!node.isDistinct()) {
                    builder.append("ALL ");
                }
            }
        }

        return null;
    }

    @Override
    protected Void visitCreateView(CreateView node, Integer indent) {
        builder.append("CREATE ");
        if (node.isReplace()) {
            builder.append("OR REPLACE ");
        }
        builder.append("VIEW ")
                .append(formatName(node.getName()))
                .append(" AS\n");

        process(node.getQuery(), indent);

        return null;
    }

    @Override
    protected Void visitCreateFunction(CreateFunction node, Integer indent) {
        builder.append("CREATE FUNCTION ")
                .append(formatName(node.getFunctionName()))
                .append(" ")
                .append(formatSqlParameterDeclarations(node.getParameters()))
                .append("\nRETURNS ")
                .append(node.getReturnType());
        if (node.getComment().isPresent()) {
            builder.append("\nCOMMENT ")
                    .append(formatStringLiteral(node.getComment().get()));
        }
        builder.append("\n")
                .append(formatRoutineCharacteristics(node.getCharacteristics()))
                .append("\n");

        process(node.getBody(), 0);

        return null;
    }

    @Override
    protected Void visitAlterFunction(AlterFunction node, Integer indent) {
        builder.append("ALTER FUNCTION ")
                .append(formatName(node.getFunctionName()));
        node.getParameterTypes().map(BaseSqlFormatter::formatTypeList).ifPresent(builder::append);
        builder.append("\n")
                .append(formatAlterRoutineCharacteristics(node.getCharacteristics()));

        return null;
    }

    @Override
    protected Void visitDropFunction(DropFunction node, Integer indent) {
        builder.append("DROP FUNCTION ");
        if (node.isExists()) {
            builder.append("IF EXISTS ");
        }
        builder.append(formatName(node.getFunctionName()));
        node.getParameterTypes().map(BaseSqlFormatter::formatTypeList).ifPresent(builder::append);

        return null;
    }

    @Override
    protected Void visitReturn(Return node, Integer indent) {
        append(indent, "RETURN ");
        builder.append(formatExpression(node.getExpression(), parameters));

        return null;
    }

    @Override
    protected Void visitDropView(DropView node, Integer context) {
        builder.append("DROP VIEW ");
        if (node.isExists()) {
            builder.append("IF EXISTS ");
        }
        builder.append(node.getName());

        return null;
    }

    @Override
    protected Void visitExplain(Explain node, Integer indent) {
        builder.append("EXPLAIN ");
        if (node.isAnalyze()) {
            builder.append("ANALYZE ");
        }

        List<String> options = new ArrayList<>();

        for (ExplainOption option : node.getOptions()) {
            if (option instanceof ExplainType) {
                options.add("TYPE " + ((ExplainType) option).getType());
            } else if (option instanceof ExplainFormat) {
                options.add("FORMAT " + ((ExplainFormat) option).getType());
            } else {
                throw new UnsupportedOperationException("unhandled explain option: " + option);
            }
        }

        if (!options.isEmpty()) {
            builder.append("(");
            Joiner.on(", ").appendTo(builder, options);
            builder.append(")");
        }

        builder.append("\n");

        process(node.getStatement(), indent);

        return null;
    }

    @Override
    protected Void visitShowCatalogs(ShowCatalogs node, Integer context) {
        builder.append("SHOW CATALOGS");

        node.getLikePattern().ifPresent((value) ->
                builder.append(" LIKE ")
                        .append(formatStringLiteral(value)));

        return null;
    }

    @Override
    protected Void visitShowSchemas(ShowSchemas node, Integer context) {
        builder.append("SHOW SCHEMAS");

        if (node.getCatalog().isPresent()) {
            builder.append(" FROM ")
                    .append(node.getCatalog().get());
        }

        node.getLikePattern().ifPresent((value) ->
                builder.append(" LIKE ")
                        .append(formatStringLiteral(value)));

        node.getEscape().ifPresent((value) ->
                builder.append(" ESCAPE ")
                        .append(formatStringLiteral(value)));

        return null;
    }

    @Override
    protected Void visitShowTables(ShowTables node, Integer context) {
        builder.append("SHOW TABLES");

        node.getSchema().ifPresent(value ->
                builder.append(" FROM ")
                        .append(formatName(value)));

        node.getLikePattern().ifPresent(value ->
                builder.append(" LIKE ")
                        .append(formatStringLiteral(value)));

        node.getEscape().ifPresent(value ->
                builder.append(" ESCAPE ")
                        .append(formatStringLiteral(value)));

        return null;
    }

    @Override
    protected Void visitShowCreate(ShowCreate node, Integer context) {
        if (node.getType() == ShowCreate.Type.TABLE) {
            builder.append("SHOW CREATE TABLE ")
                    .append(formatName(node.getName()));
        } else if (node.getType() == ShowCreate.Type.VIEW) {
            builder.append("SHOW CREATE VIEW ")
                    .append(formatName(node.getName()));
        }

        return null;
    }

    @Override
    protected Void visitShowCreateFunction(ShowCreateFunction node, Integer context) {
        builder.append("SHOW CREATE FUNCTION ")
                .append(formatName(node.getName()));
        node.getParameterTypes().map(BaseSqlFormatter::formatTypeList).ifPresent(builder::append);

        return null;
    }

    @Override
    protected Void visitShowColumns(ShowColumns node, Integer context) {
        builder.append("SHOW COLUMNS FROM ")
                .append(formatName(node.getTable()));

        return null;
    }

    @Override
    protected Void visitShowStats(ShowStats node, Integer context) {
        builder.append("SHOW STATS FOR ");
        process(node.getRelation(), 0);
        builder.append("");
        return null;
    }

    @Override
    protected Void visitShowFunctions(ShowFunctions node, Integer context) {
        builder.append("SHOW FUNCTIONS");

        return null;
    }

    @Override
    protected Void visitShowSession(ShowSession node, Integer context) {
        builder.append("SHOW SESSION");

        return null;
    }

    @Override
    protected Void visitDelete(Delete node, Integer context) {
        builder.append("DELETE FROM ")
                .append(formatName(node.getTable().getName()));

        if (node.getWhere().isPresent()) {
            builder.append(" WHERE ")
                    .append(formatExpression(node.getWhere().get(), parameters));
        }

        return null;
    }

    @Override
    protected Void visitCreateSchema(CreateSchema node, Integer context) {
        builder.append("CREATE SCHEMA ");
        if (node.isNotExists()) {
            builder.append("IF NOT EXISTS ");
        }
        builder.append(formatName(node.getSchemaName()));
        builder.append(formatPropertiesMultiLine(node.getProperties()));

        return null;
    }

    @Override
    protected Void visitDropSchema(DropSchema node, Integer context) {
        builder.append("DROP SCHEMA ");
        if (node.isExists()) {
            builder.append("IF EXISTS ");
        }
        builder.append(formatName(node.getSchemaName()))
                .append(" ")
                .append(node.isCascade() ? "CASCADE" : "RESTRICT");

        return null;
    }

    @Override
    protected Void visitRenameSchema(RenameSchema node, Integer context) {
        builder.append("ALTER SCHEMA ")
                .append(formatName(node.getSource()))
                .append(" RENAME TO ")
                .append(formatExpression(node.getTarget(), parameters));

        return null;
    }

    @Override
    protected Void visitCreateTableAsSelect(CreateTableAsSelect node, Integer indent) {
        builder.append("CREATE TABLE ");
        if (node.isNotExists()) {
            builder.append("IF NOT EXISTS ");
        }
        builder.append(formatName(node.getName()));

        if (node.getColumnAliases().isPresent()) {
            String columnList = node.getColumnAliases().get().stream().map(element -> formatExpression(element, parameters)).collect(joining(", "));
            builder.append(format("( %s )", columnList));
        }

        if (node.getComment().isPresent()) {
            builder.append("\nCOMMENT " + formatStringLiteral(node.getComment().get()));
        }

        builder.append(formatPropertiesMultiLine(node.getProperties()));

        builder.append(" AS ");
        process(node.getQuery(), indent);

        if (!node.isWithData()) {
            builder.append(" WITH NO DATA");
        }

        return null;
    }

    @Override
    protected Void visitCreateTable(CreateTable node, Integer indent) {
        builder.append("CREATE TABLE ");
        if (node.isNotExists()) {
            builder.append("IF NOT EXISTS ");
        }
        String tableName = formatName(node.getName());
        builder.append(tableName).append(" (\n");

        String elementIndent = indentString(indent + 1);
        String columnList = node.getElements().stream()
                .map(element -> {
                    if (element instanceof ColumnDefinition) {
                        ColumnDefinition column = (ColumnDefinition) element;
                        return elementIndent + formatColumnDefinition(column);
                    }
                    if (element instanceof LikeClause) {
                        LikeClause likeClause = (LikeClause) element;
                        StringBuilder builder = new StringBuilder(elementIndent);
                        builder.append("LIKE ")
                                .append(formatName(likeClause.getTableName()));
                        if (likeClause.getPropertiesOption().isPresent()) {
                            builder.append(" ")
                                    .append(likeClause.getPropertiesOption().get().name())
                                    .append(" PROPERTIES");
                        }
                        return builder.toString();
                    }
                    throw new UnsupportedOperationException("unknown table element: " + element);
                })
                .collect(joining(",\n"));
        builder.append(columnList);
        builder.append("\n").append(")");

        if (node.getComment().isPresent()) {
            builder.append("\nCOMMENT " + formatStringLiteral(node.getComment().get()));
        }

        builder.append(formatPropertiesMultiLine(node.getProperties()));

        return null;
    }

    private String formatPropertiesMultiLine(List<Property> properties) {
        if (properties.isEmpty()) {
            return "";
        }

        String propertyList = properties.stream()
                .map(element -> INDENT +
                        formatExpression(element.getName(), parameters) + " = " +
                        formatExpression(element.getValue(), parameters))
                .collect(joining(",\n"));

        return "\nWITH (\n" + propertyList + "\n)";
    }

    private String formatPropertiesSingleLine(List<Property> properties) {
        if (properties.isEmpty()) {
            return "";
        }

        String propertyList = properties.stream()
                .map(element -> formatExpression(element.getName(), parameters) + " = " +
                        formatExpression(element.getValue(), parameters))
                .collect(joining(", "));

        return " WITH ( " + propertyList + " )";
    }

    private String formatSqlParameterDeclarations(List<SqlParameterDeclaration> parameters) {
        if (parameters.isEmpty()) {
            return "()";
        }
        return parameters.stream()
                .map(parameter -> format(
                        "%s%s %s",
                        INDENT,
                        formatExpression(parameter.getName(), this.parameters),
                        parameter.getType()))
                .collect(joining(",\n", "(\n", "\n)"));
    }

    private static String formatTypeList(List<String> types) {
        return format("(%s)", Joiner.on(", ").join(types));
    }

    private String formatRoutineCharacteristics(RoutineCharacteristics characteristics) {
        return Joiner.on("\n").join(ImmutableList.of(
                "LANGUAGE " + formatRoutineCharacteristicName(characteristics.getLanguage()),
                formatRoutineCharacteristicName(characteristics.getDeterminism()),
                formatRoutineCharacteristicName(characteristics.getNullCallClause())));
    }

    private String formatAlterRoutineCharacteristics(AlterRoutineCharacteristics characteristics) {
        StringBuilder formatted = new StringBuilder();
        if (characteristics.getNullCallClause().isPresent()) {
            formatted.append(formatRoutineCharacteristicName(characteristics.getNullCallClause().get()));
        }
        return formatted.toString();
    }

    private String formatRoutineCharacteristicName(Enum characteristic) {
        return characteristic.name().replace("_", " ");
    }

    private static String formatName(String name) {
        if (NAME_PATTERN.matcher(name).matches()) {
            return name;
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String formatName(QualifiedName name) {
        return name.getOriginalParts().stream()
                .map(BaseSqlFormatter::formatName)
                .collect(joining("."));
    }

    private String formatColumnDefinition(ColumnDefinition column) {
        StringBuilder sb = new StringBuilder()
                .append(formatExpression(column.getName(), parameters))
                .append(" ").append(column.getType());
        if (!column.isNullable()) {
            sb.append(" NOT NULL");
        }
        column.getComment().ifPresent(comment ->
                sb.append(" COMMENT ").append(formatStringLiteral(comment)));
        sb.append(formatPropertiesSingleLine(column.getProperties()));
        return sb.toString();
    }

    private static String formatGrantor(GrantorSpecification grantor) {
        GrantorSpecification.Type type = grantor.getType();
        switch (type) {
            case CURRENT_ROLE:
            case CURRENT_USER:
                return type.name();
            case PRINCIPAL:
                return formatPrincipal(grantor.getPrincipal().get());
            default:
                throw new IllegalArgumentException("Unsupported principal type: " + type);
        }
    }

    private static String formatPrincipal(PrincipalSpecification principal) {
        PrincipalSpecification.Type type = principal.getType();
        switch (type) {
            case UNSPECIFIED:
                return principal.getName().toString();
            case USER:
            case ROLE:
                return format("%s %s", type.name(), principal.getName().toString());
            default:
                throw new IllegalArgumentException("Unsupported principal type: " + type);
        }
    }

    @Override
    protected Void visitDropTable(DropTable node, Integer context) {
        builder.append("DROP TABLE ");
        if (node.isExists()) {
            builder.append("IF EXISTS ");
        }
        builder.append(node.getTableName());

        return null;
    }

    @Override
    protected Void visitRenameTable(RenameTable node, Integer context) {
        builder.append("ALTER TABLE ")
                .append(node.getSource())
                .append(" RENAME TO ")
                .append(node.getTarget());

        return null;
    }

    @Override
    protected Void visitRenameColumn(RenameColumn node, Integer context) {
        builder.append("ALTER TABLE ")
                .append(node.getTable())
                .append(" RENAME COLUMN ")
                .append(node.getSource())
                .append(" TO ")
                .append(node.getTarget());

        return null;
    }

    @Override
    protected Void visitDropColumn(DropColumn node, Integer context) {
        builder.append("ALTER TABLE ")
                .append(formatName(node.getTable()))
                .append(" DROP COLUMN ")
                .append(formatExpression(node.getColumn(), parameters));

        return null;
    }

    @Override
    protected Void visitAnalyze(Analyze node, Integer context) {
        builder.append("ANALYZE ")
                .append(formatName(node.getTableName()));
        builder.append(formatPropertiesMultiLine(node.getProperties()));
        return null;
    }

    @Override
    protected Void visitAddColumn(AddColumn node, Integer indent) {
        builder.append("ALTER TABLE ")
                .append(node.getName())
                .append(" ADD COLUMN ")
                .append(formatColumnDefinition(node.getColumn()));

        return null;
    }

    @Override
    protected Void visitInsert(Insert node, Integer indent) {
        if (node.getType() == Insert.InsertType.OVERWRITE) {
            throw new UnsupportedSqlException("[base] Not supported 'INSERT OVERWRITE'");
        }
        builder.append("INSERT INTO ")
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
    public Void visitSetSession(SetSession node, Integer context) {
        builder.append("SET SESSION ")
                .append(node.getName())
                .append(" = ")
                .append(formatExpression(node.getValue(), parameters));

        return null;
    }

    @Override
    public Void visitResetSession(ResetSession node, Integer context) {
        builder.append("RESET SESSION ")
                .append(node.getName());

        return null;
    }

    @Override
    protected Void visitCallArgument(CallArgument node, Integer indent) {
        if (node.getName().isPresent()) {
            builder.append(node.getName().get())
                    .append(" => ");
        }
        builder.append(formatExpression(node.getValue(), parameters));

        return null;
    }

    @Override
    protected Void visitCall(Call node, Integer indent) {
        builder.append("CALL ")
                .append(node.getName())
                .append("(");

        Iterator<CallArgument> arguments = node.getArguments().iterator();
        while (arguments.hasNext()) {
            process(arguments.next(), indent);
            if (arguments.hasNext()) {
                builder.append(", ");
            }
        }

        builder.append(")");

        return null;
    }

    @Override
    protected Void visitRow(Row node, Integer indent) {
        builder.append("ROW(");
        boolean firstItem = true;
        for (Expression item : node.getItems()) {
            if (!firstItem) {
                builder.append(", ");
            }
            process(item, indent);
            firstItem = false;
        }
        builder.append(")");
        return null;
    }

    @Override
    protected Void visitStartTransaction(StartTransaction node, Integer indent) {
        builder.append("START TRANSACTION");

        Iterator<TransactionMode> iterator = node.getTransactionModes().iterator();
        while (iterator.hasNext()) {
            builder.append(" ");
            process(iterator.next(), indent);
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        return null;
    }

    @Override
    protected Void visitIsolationLevel(Isolation node, Integer indent) {
        builder.append("ISOLATION LEVEL ").append(node.getLevel().getText());
        return null;
    }

    @Override
    protected Void visitTransactionAccessMode(TransactionAccessMode node, Integer context) {
        builder.append(node.isReadOnly() ? "READ ONLY" : "READ WRITE");
        return null;
    }

    @Override
    protected Void visitCommit(Commit node, Integer context) {
        builder.append("COMMIT");
        return null;
    }

    @Override
    protected Void visitRollback(Rollback node, Integer context) {
        builder.append("ROLLBACK");
        return null;
    }

    @Override
    protected Void visitCreateRole(CreateRole node, Integer context) {
        builder.append("CREATE ROLE ").append(node.getName());
        if (node.getGrantor().isPresent()) {
            builder.append(" WITH ADMIN ").append(formatGrantor(node.getGrantor().get()));
        }
        return null;
    }

    @Override
    protected Void visitDropRole(DropRole node, Integer context) {
        builder.append("DROP ROLE ").append(node.getName());
        return null;
    }

    @Override
    protected Void visitGrantRoles(GrantRoles node, Integer context) {
        builder.append("GRANT ");
        builder.append(node.getRoles().stream()
                .map(Identifier::toString)
                .collect(joining(", ")));
        builder.append(" TO ");
        builder.append(node.getGrantees().stream()
                .map(BaseSqlFormatter::formatPrincipal)
                .collect(joining(", ")));
        if (node.isWithAdminOption()) {
            builder.append(" WITH ADMIN OPTION");
        }
        if (node.getGrantor().isPresent()) {
            builder.append(" GRANTED BY ").append(formatGrantor(node.getGrantor().get()));
        }
        return null;
    }

    @Override
    protected Void visitRevokeRoles(RevokeRoles node, Integer context) {
        builder.append("REVOKE ");
        if (node.isAdminOptionFor()) {
            builder.append("ADMIN OPTION FOR ");
        }
        builder.append(node.getRoles().stream()
                .map(Identifier::toString)
                .collect(joining(", ")));
        builder.append(" FROM ");
        builder.append(node.getGrantees().stream()
                .map(BaseSqlFormatter::formatPrincipal)
                .collect(joining(", ")));
        if (node.getGrantor().isPresent()) {
            builder.append(" GRANTED BY ").append(formatGrantor(node.getGrantor().get()));
        }
        return null;
    }

    @Override
    protected Void visitSetRole(SetRole node, Integer context) {
        builder.append("SET ROLE ");
        SetRole.Type type = node.getType();
        switch (type) {
            case ALL:
            case NONE:
                builder.append(type.toString());
                break;
            case ROLE:
                builder.append(node.getRole().get());
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return null;
    }

    @Override
    public Void visitGrant(Grant node, Integer indent) {
        builder.append("GRANT ");

        if (node.getPrivileges().isPresent()) {
            builder.append(node.getPrivileges().get().stream()
                    .collect(joining(", ")));
        } else {
            builder.append("ALL PRIVILEGES");
        }

        builder.append(" ON ");
        if (node.isTable()) {
            builder.append("TABLE ");
        }
        builder.append(node.getTableName())
                .append(" TO ")
                .append(formatPrincipal(node.getGrantee()));
        if (node.isWithGrantOption()) {
            builder.append(" WITH GRANT OPTION");
        }

        return null;
    }

    @Override
    public Void visitRevoke(Revoke node, Integer indent) {
        builder.append("REVOKE ");

        if (node.isGrantOptionFor()) {
            builder.append("GRANT OPTION FOR ");
        }

        if (node.getPrivileges().isPresent()) {
            builder.append(node.getPrivileges().get().stream()
                    .collect(joining(", ")));
        } else {
            builder.append("ALL PRIVILEGES");
        }

        builder.append(" ON ");
        if (node.isTable()) {
            builder.append("TABLE ");
        }
        builder.append(node.getTableName())
                .append(" FROM ")
                .append(formatPrincipal(node.getGrantee()));

        return null;
    }

    @Override
    public Void visitShowGrants(ShowGrants node, Integer indent) {
        builder.append("SHOW GRANTS ");

        if (node.getTableName().isPresent()) {
            builder.append("ON ");

            if (node.getTable()) {
                builder.append("TABLE ");
            }
            builder.append(node.getTableName().get());
        }

        return null;
    }

    @Override
    protected Void visitShowRoles(ShowRoles node, Integer context) {
        builder.append("SHOW ");
        if (node.isCurrent()) {
            builder.append("CURRENT ");
        }
        builder.append("ROLES");

        if (node.getCatalog().isPresent()) {
            builder.append(" FROM ")
                    .append(node.getCatalog().get());
        }

        return null;
    }

    @Override
    protected Void visitShowRoleGrants(ShowRoleGrants node, Integer context) {
        builder.append("SHOW ROLE GRANTS");

        if (node.getCatalog().isPresent()) {
            builder.append(" FROM ")
                    .append(node.getCatalog().get());
        }

        return null;
    }

    private void processRelation(Relation relation, Integer indent) {
        // TODO: handle this properly
        if (relation instanceof Table) {
            builder.append("TABLE ")
                    .append(((Table) relation).getName())
                    .append('\n');
        } else {
            process(relation, indent);
        }
    }

    public StringBuilder append(int indent, String value) {
        return builder.append(indentString(indent))
                .append(value);
    }

    private static String indentString(int indent) {
        return Strings.repeat(INDENT, indent);
    }

    private void appendAliasColumns(StringBuilder builder, List<Identifier> columns) {
        if ((columns != null) && (!columns.isEmpty())) {
            String formattedColumns = columns.stream()
                    .map(name -> formatExpression(name, Optional.empty()))
                    .collect(Collectors.joining(", "));

            builder.append(" (")
                    .append(formattedColumns)
                    .append(')');
        }
    }
}