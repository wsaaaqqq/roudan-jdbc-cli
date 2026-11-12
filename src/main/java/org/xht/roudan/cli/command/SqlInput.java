package org.xht.roudan.cli.command;

import cn.hutool.core.io.FileUtil;

final class SqlInput {

    private SqlInput() {
    }

    static String read(String sql, String file) {
        if (sql == null && file == null) {
            throw new IllegalArgumentException("Missing SQL. Use -s/--sql \"...\" or -f <sql-file>.");
        }
        String resolved = sql != null ? sql : FileUtil.readUtf8String(file);
        if (resolved.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL is empty. Use -s/--sql \"...\" or -f <sql-file> with non-empty SQL.");
        }
        return resolved;
    }
}
