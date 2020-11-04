# Keywords
| name             | hive | presto | explain |
| ---------------- | ---- | ------ | ------- |
| INSERT OVERWRITE | 支持 | 不支持 |         |
| CLUSTER BY       | 支持 | 不支持 |         |
| DISTRIBUTE BY    | 支持 | 不支持 |         |
| SORT BY          | 支持 | 不支持 |         |
| RLIKE 正则匹配   | 支持 | 不支持 |         |
| Bit运算   | 与/或/非/异或 | 不支持 | 支持与、或、非、异或(!)和左右移位操作 |
- - -

# Array Function
| name     | hive       | presto     | explain                                 |
| -------- | ---------- | ---------- | --------------------------------------- |
| 创建数组 | array(1,2) | array[1,2] | 默认使用"()"                            |
| 数组索引 | array[0]   | array[1]   | 访问数组索引起始位置不一致(默认从0开始) |
- - -
# String Function
| name     | hive        | presto           | explain                |
| -------- | ----------- | ---------------- | ---------------------- |
| 正则匹配 | regexp(A,B) | regexp_like(A,B) | 不支持REGEXP关键字语法 |
| json解析 | get_json_object("json_string","json_path") | json_extract_scalar("json_string","json_path") |  |
- - -
# Date Function
| name     | hive       | presto     | explain                                 |
| -------- | ---------- | ---------- | --------------------------------------- |
| 转换日期格式 |from_unixtime(bigint,string) | format_datetime(from_unixtime(bigint),string) |  |