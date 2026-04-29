package org.yechan.remittance

import com.p6spy.engine.spy.appender.MessageFormattingStrategy

class P6SpySqlFormatter : MessageFormattingStrategy {

    override fun formatMessage(
        connectionId: Int,
        now: String?,
        elapsed: Long,
        category: String?,
        prepared: String?,
        sql: String?,
        url: String?,
    ): String {
        val normalizedSql = sql.normalizeSql()
        if (normalizedSql.isBlank()) {
            return ""
        }

        return "SQL category=${category.orEmpty()} elapsed=${elapsed}ms connection=$connectionId\n" +
            normalizedSql.formatSql()
    }

    private fun String?.normalizeSql(): String = this
        ?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.joinToString(" ")
        .orEmpty()

    private fun String.formatSql(): String = formatInsertSql()
        ?: formatSelectSql()
        ?: formatUpdateSql()
        ?: sqlLineBreakRules.fold(this) { formatted, rule ->
            formatted.replace(rule.pattern, rule.replacement)
        }.trim()

    private fun String.formatInsertSql(): String? {
        val match = insertSqlRegex.matchEntire(this) ?: return null
        val columns = match.groupValues[2].formatCommaSeparatedLines()
        val values = match.groupValues[3].formatCommaSeparatedLines()

        return "${match.groupValues[1]} (\n" +
            "$columns\n" +
            ")\n" +
            "values (\n" +
            "$values\n" +
            ")"
    }

    private fun String.formatSelectSql(): String? {
        val match = selectSqlRegex.matchEntire(this) ?: return null

        val columns = match.groupValues[1].formatCommaSeparatedLines()
        val rest = match.groupValues[2]

        val formattedRest = sqlLineBreakRules.fold("from $rest") { formatted, rule ->
            formatted.replace(rule.pattern, rule.replacement)
        }.trim().formatTableAliases()

        return "select\n" +
            "$columns\n" +
            formattedRest
    }

    private fun String.formatUpdateSql(): String? {
        val match = updateSqlRegex.matchEntire(this) ?: return null
        val target = match.groupValues[1].formatTableAlias()
        val assignments = match.groupValues[2].formatCommaSeparatedLines()
        val whereClause = match.groupValues[3].takeIf { it.isNotBlank() }
            ?.let { clause ->
                sqlLineBreakRules.fold(clause.trim()) { formatted, rule ->
                    formatted.replace(rule.pattern, rule.replacement)
                }.trim().formatTableAliases()
            }
            ?.let { "\n$it" }
            .orEmpty()

        return "$target\n" +
            "set\n" +
            assignments +
            whereClause
    }

    private fun String.formatCommaSeparatedLines(): String = splitSqlCommaAware()
        .joinToString(",\n") { value -> "  ${value.trim()}" }

    private fun String.splitSqlCommaAware(): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var inSingleQuote = false
        var index = 0

        while (index < length) {
            val char = this[index]
            when (char) {
                '\'' -> {
                    current.append(char)
                    if (inSingleQuote && getOrNull(index + 1) == '\'') {
                        current.append('\'')
                        index += 1
                    } else {
                        inSingleQuote = !inSingleQuote
                    }
                }

                '(' -> {
                    if (!inSingleQuote) {
                        depth++
                    }
                    current.append(char)
                }

                ')' -> {
                    if (!inSingleQuote) {
                        depth--
                    }
                    current.append(char)
                }

                ',' -> {
                    if (depth == 0 && !inSingleQuote) {
                        result += current.toString()
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }

                else -> current.append(char)
            }
            index += 1
        }

        if (current.isNotBlank()) {
            result += current.toString()
        }

        return result
    }

    private fun String.formatTableAliases(): String = lineSequence()
        .joinToString("\n") { line -> line.formatTableAlias() }

    private fun String.formatTableAlias(): String {
        val match = tableAliasRegex.matchEntire(this) ?: return this
        val suffix = match.groupValues[4]
        if (suffix.trimStart().startsWith("is ", ignoreCase = true)) {
            return this
        }
        return "${match.groupValues[1]} ${match.groupValues[2]}\n  ${match.groupValues[3]}$suffix"
    }

    private data class SqlLineBreakRule(
        val pattern: Regex,
        val replacement: String,
    )

    companion object {
        private val insertSqlRegex = Regex(
            """(?is)^(insert(?:\s+ignore)?\s+into\s+\S+)\s*\((.*)\)\s+values\s*\((.*)\)$""",
        )

        private val selectSqlRegex = Regex(
            """(?is)^select\s+(.*?)\s+from\s+(.+)$""",
        )

        private val updateSqlRegex = Regex(
            """(?is)^(update\s+\S+(?:\s+\S+)?)\s+set\s+(.+?)(\s+where\s+.*)?$""",
        )

        private val tableAliasRegex = Regex(
            """(?i)^(from|update|join|left join|right join|inner join)\s+(\S+)\s+(\S+)(.*)$""",
        )

        private val sqlLineBreakRules = listOf(
            SqlLineBreakRule(Regex("""\s+(from)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(
                Regex(
                    """\s+(left join|right join|inner join|join)\s+""",
                    RegexOption.IGNORE_CASE,
                ),
                "\n$1 ",
            ),
            SqlLineBreakRule(Regex("""\s+(where)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(and|or)\s+""", RegexOption.IGNORE_CASE), "\n  $1 "),
            SqlLineBreakRule(Regex("""\s+(group by)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(having)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(order by)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(limit)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(values)\s*""", RegexOption.IGNORE_CASE), "\n$1 "),
            SqlLineBreakRule(Regex("""\s+(set)\s+""", RegexOption.IGNORE_CASE), "\n$1 "),
        )
    }
}
