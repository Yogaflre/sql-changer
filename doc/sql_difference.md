# Keywords
| name             | hive | presto | explain |
| ---------------- | ---- | ------ | ------- |
| INSERT OVERWRITE | 支持 | 不支持 |         |
| CLUSTER BY       | 支持 | 不支持 |         |
| DISTRIBUTE BY    | 支持 | 不支持 |         |
| SORT BY          | 支持 | 不支持 |         |
| RLIKE 正则匹配   | 支持 | 不支持 |         |
- - -

# Array Function

| name     | hive       | presto     | explain                                 |
| -------- | ---------- | ---------- | --------------------------------------- |
| 创建数组 | array(1,2) | array[1,2] | 默认使用"[]"                            |
| 数组索引 | array[0]   | array[1]   | 访问数组索引起始位置不一致(默认从0开始) |

# Regex Function
| name     | hive | presto | explain |
| -------- | ---- | ------ | ------- |
| 正则匹配 |      |        |         |