/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yogafire.sql.changer;

import com.yogafire.sql.changer.formatter.hive.HiveSqlFormatter;
import com.yogafire.sql.changer.formatter.presto.PrestoSqlFormatter;
import com.yogafire.sql.changer.formatter.base.BaseSqlFormatter;
import com.yogafire.sql.changer.formatter.base.SqlType;
import com.yogafire.sql.changer.parser.ParsingOptions;
import com.yogafire.sql.changer.parser.SqlParser;
import com.yogafire.sql.changer.tree.Expression;
import com.yogafire.sql.changer.tree.Node;
import com.yogafire.sql.changer.tree.Statement;

import java.util.List;
import java.util.Optional;

public final class SqlFormatter {

    private SqlFormatter() {
    }

    public static String change(String sql, SqlType type) {
        Statement statement = new SqlParser().createStatement(sql, ParsingOptions.builder()
                .setDecimalLiteralTreatment(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL).build());
        return formatSql(statement, type, Optional.empty());
    }

    public static String formatSql(Node root, Optional<List<Expression>> parameters) {
        return formatSql(root, SqlType.OTHER, parameters);
    }

    public static String formatSql(Node root, SqlType type, Optional<List<Expression>> parameters) {
        StringBuilder builder = new StringBuilder();
        switch (type) {
            case PRESTO_SQL:
                new PrestoSqlFormatter(builder, parameters).process(root, 0);
                break;
            case HIVE_SQL:
                new HiveSqlFormatter(builder, parameters).process(root, 0);
                break;
            default:
                new BaseSqlFormatter(builder, parameters).process(root, 0);
        }
        return builder.toString();
    }
}
