package org.xht.roudan.cli;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class CliRegressionTest {

    private static final String URL = "jdbc:h2:mem:cli_regression;DB_CLOSE_DELAY=-1";
    private static final String DRIVER_JAR = driverJar();

    private static String driverJar() {
        try {
            return new File(org.h2.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        RD.setConnection(null);
        Main.clearTxConnection();
        RDConfig.setAutoClose(true);
        RDConfig.setAutoCommit(true);
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("CREATE TABLE T_USER (ID INT PRIMARY KEY, YHM VARCHAR(32), ZSXM VARCHAR(32), ZT INT, SMZQ VARCHAR(1))");
            stmt.execute("INSERT INTO T_USER VALUES (1, 'alice', 'Alice', 1, 'T')");
        }
    }

    @After
    public void tearDown() {
        RD.setConnection(null);
        Main.clearTxConnection();
        RDConfig.setAutoClose(true);
        RDConfig.setAutoCommit(true);
    }

    @Test
    public void allSubcommandsSupportHelpWithoutConnecting() throws Exception {
        for (String name : new CommandLine(new Main()).getSubcommands().keySet()) {
            if ("help".equals(name)) continue;
            for (String option : Arrays.asList("--help", "-h")) {
                Output output = run("-u", "jdbc:invalid:must-not-connect", name, option);
                assertEquals(name + " " + option + ": " + output.stdout, 0, output.exitCode);
                assertTrue(name + ": " + output.stdout, output.stdout.contains("Usage:"));
            }
        }
    }

    @Test
    public void missingSqlIsValidatedBeforeConnectionInitialization() throws Exception {
        for (String name : Arrays.asList("query", "modify", "count", "exec")) {
            Output output = run("-u", "jdbc:invalid:must-not-connect", name);
            assertEquals(1, output.exitCode);
            JSONObject json = output.json();
            assertEquals("PARAM_ERROR", json.getStr("errorCode"));
            assertTrue(json.getStr("error"), json.getStr("error").contains("-s"));
            assertTrue(json.getStr("error"), json.getStr("error").contains("-f"));
            assertFalse(output.stdout.contains("NullPointerException"));
        }
    }

    @Test
    public void blankSqlAndEmptySqlFilesAreRejectedBeforeConnecting() throws Exception {
        File file = File.createTempFile("roudan-empty-", ".sql");
        try {
            Files.write(file.toPath(), " \n\t".getBytes(StandardCharsets.UTF_8));
            for (String name : Arrays.asList("query", "modify", "count", "exec")) {
                for (String[] source : Arrays.asList(new String[]{"-s", " "}, new String[]{"-f", file.getAbsolutePath()})) {
                    Output output = run("-u", "jdbc:invalid:must-not-connect", name, source[0], source[1]);
                    assertEquals(1, output.exitCode);
                    assertEquals("PARAM_ERROR", output.json().getStr("errorCode"));
                }
            }
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void jsonQueryPreservesSelectOrderAndValues() throws Exception {
        Output output = database("query", "-s", "SELECT SMZQ, ID, YHM, ZSXM, ZT FROM T_USER");
        assertEquals(output.stdout, 0, output.exitCode);
        assertEquals(Arrays.asList("SMZQ", "ID", "YHM", "ZSXM", "ZT"), output.json().getJSONArray("cols").toList(String.class));
        JSONArray row = output.json().getJSONArray("rows").getJSONArray(0);
        assertEquals("T", row.getStr(0));
        assertEquals(Integer.valueOf(1), row.getInt(1));
        assertEquals("alice", row.getStr(2));
    }

    @Test
    public void namedAndPositionalQueriesPreserveAliasesAndNativeTypes() throws Exception {
        for (String[] args : Arrays.asList(
                new String[]{"query", "-s", "SELECT YHM AS \"login\", ID, CAST(NULL AS INT) AS NOTHING FROM T_USER WHERE ID=?", "-a", "[1]"},
                new String[]{"query", "-s", "SELECT YHM AS \"login\", ID, CAST(NULL AS INT) AS NOTHING FROM T_USER WHERE ID=:id", "--named", "-a", "{\"id\":1}"})) {
            Output output = database(args);
            assertEquals(output.stdout, 0, output.exitCode);
            assertEquals(Arrays.asList("login", "ID", "NOTHING"), output.json().getJSONArray("cols").toList(String.class));
            JSONArray row = output.json().getJSONArray("rows").getJSONArray(0);
            assertEquals("alice", row.getStr(0));
            assertTrue(row.get(1) instanceof Number);
            assertTrue(JSONUtil.isNull(row.get(2)));
        }
    }

    @Test
    public void queriesKeepValuesForDuplicateColumnLabelsAndLobs() throws Exception {
        Output output = database("query", "-s", "SELECT ID AS V, ID+1 AS V, CAST('text' AS CLOB) AS CONTENT FROM T_USER");
        assertEquals(output.stdout, 0, output.exitCode);
        assertEquals(Arrays.asList("V", "V", "CONTENT"), output.json().getJSONArray("cols").toList(String.class));
        JSONArray row = output.json().getJSONArray("rows").getJSONArray(0);
        assertEquals(Integer.valueOf(1), row.getInt(0));
        assertEquals(Integer.valueOf(2), row.getInt(1));
        assertEquals("text", row.getStr(2));
    }

    @Test
    public void tableAndCsvPreserveSelectOrder() throws Exception {
        Output table = database("query", "-s", "SELECT SMZQ, ID, YHM, ZSXM, ZT FROM T_USER", "-o", "table");
        assertEquals(table.stdout, 0, table.exitCode);
        String header = table.stdout.split("\\R")[1];
        assertTrue(header, header.indexOf("SMZQ") < header.indexOf("ID"));
        assertTrue(header, header.indexOf("ID") < header.indexOf("YHM"));
        assertTrue(header, header.indexOf("YHM") < header.indexOf("ZSXM"));
        assertTrue(header, header.indexOf("ZSXM") < header.indexOf("ZT"));

        Output csv = database("query", "-s", "SELECT SMZQ, ID, YHM FROM T_USER", "-o", "csv");
        assertEquals(csv.stdout, 0, csv.exitCode);
        assertTrue(csv.stdout, csv.stdout.startsWith("SMZQ,ID,YHM\nT,1,alice"));
    }

    @Test
    public void emptyTableOutputIsReadableWithOrWithoutHeaders() throws Exception {
        for (String[] args : Arrays.asList(
                new String[]{"-o", "table", "query", "-s", "SELECT ID FROM T_USER WHERE ID=99"},
                new String[]{"-o", "table", "--no-header", "query", "-s", "SELECT ID FROM T_USER WHERE ID=99"},
                new String[]{"-o", "table", "query", "-s", "SELECT ID FROM T_USER", "--limit", "0"})) {
            Output output = database(args);
            assertEquals(output.stdout, 0, output.exitCode);
            assertEquals("0 rows", output.stdout.trim());
        }
    }

    @Test
    public void emptyResultWriterDoesNotRenderBrokenBorders() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(bytes, true, "UTF-8"));
            ResultWriter.printQueryResult(Collections.emptyList(), Collections.emptyList(), 0, 0, "table", false, false);
        } finally {
            System.setOut(original);
        }
        assertEquals("0 rows", bytes.toString("UTF-8").trim());
    }

    @Test
    public void emptyAndPagedQueriesKeepColumnMetadataAndTotalCount() throws Exception {
        Output empty = database("query", "-s", "SELECT YHM, ID FROM T_USER WHERE ID=99");
        assertEquals(empty.stdout, 0, empty.exitCode);
        assertEquals(Arrays.asList("YHM", "ID"), empty.json().getJSONArray("cols").toList(String.class));
        assertTrue(empty.json().getJSONArray("rows").isEmpty());
        assertEquals(Integer.valueOf(0), empty.json().getInt("rowCount"));

        Output paged = database("query", "-s", "SELECT YHM, ID FROM T_USER", "--page", "2", "--size", "1");
        assertEquals(paged.stdout, 0, paged.exitCode);
        assertEquals(Arrays.asList("YHM", "ID"), paged.json().getJSONArray("cols").toList(String.class));
        assertTrue(paged.json().getJSONArray("rows").isEmpty());
        assertEquals(Integer.valueOf(1), paged.json().getInt("rowCount"));
    }

    @Test
    public void outputAndPrettyFlagsWorkAfterSubcommand() throws Exception {
        Output table = database("query", "-s", "SELECT YHM, ID FROM T_USER", "-o", "table");
        assertEquals(table.stdout, 0, table.exitCode);
        assertTrue(table.stdout.contains("YHM"));
        assertFalse(table.stdout.startsWith("{"));

        Output empty = database("query", "-s", "SELECT YHM FROM T_USER WHERE ID=99", "-o", "table", "--no-header");
        assertEquals(empty.stdout, 0, empty.exitCode);
        assertEquals("0 rows", empty.stdout.trim());

        Output pretty = database("query", "-s", "SELECT YHM FROM T_USER", "--pretty");
        assertEquals(pretty.stdout, 0, pretty.exitCode);
        assertTrue(pretty.stdout.contains("\n"));

        Output execPretty = database("exec", "-s", "SELECT * FROM T_USER", "--pretty");
        assertEquals(execPretty.stdout, 0, execPretty.exitCode);
        assertTrue(execPretty.stdout.contains("\n"));
    }

    @Test
    public void describeSupportsTableFormatBeforeAndAfterSubcommand() throws Exception {
        for (String[] args : Arrays.asList(
                new String[]{"-o", "table", "describe", "-t", "T_USER"},
                new String[]{"describe", "-t", "T_USER", "-o", "table"})) {
            Output output = database(args);
            assertEquals(output.stdout, 0, output.exitCode);
            assertTrue(output.stdout, output.stdout.contains("NAME"));
            assertTrue(output.stdout, output.stdout.contains("TYPE"));
            assertTrue(output.stdout, output.stdout.contains("SIZE"));
            assertTrue(output.stdout, output.stdout.contains("NULLABLE"));
            assertTrue(output.stdout, output.stdout.contains("PK"));
            assertTrue(output.stdout, output.stdout.contains("YHM"));
            assertFalse(output.stdout.trim().startsWith("{"));
        }
    }

    @Test
    public void describeKeepsExistingJsonSchema() throws Exception {
        Output output = database("describe", "-t", "T_USER");
        assertEquals(output.stdout, 0, output.exitCode);
        JSONObject json = output.json();
        assertEquals("T_USER", json.getStr("table"));
        assertEquals(5, json.getJSONArray("columns").size());
        assertTrue(json.getJSONArray("columns").getJSONObject(0).getBool("pk"));
    }

    @Test
    public void describeSupportsCsvAndNoHeaderWithoutChangingPrettyJsonSchema() throws Exception {
        Output csv = database("describe", "-t", "T_USER", "-o", "csv");
        assertEquals(csv.stdout, 0, csv.exitCode);
        assertTrue(csv.stdout, csv.stdout.startsWith("NAME,TYPE,SIZE,NULLABLE,PK\nID,"));

        Output table = database("--no-header", "describe", "-t", "T_USER", "-o", "table");
        assertEquals(table.stdout, 0, table.exitCode);
        assertFalse(table.stdout.contains("NULLABLE"));
        assertTrue(table.stdout.contains("YHM"));

        Output json = database("describe", "-t", "T_USER", "-o", "json-pretty");
        assertEquals(json.stdout, 0, json.exitCode);
        assertTrue(json.stdout.trim().contains("\n"));
        assertEquals("T_USER", json.json().getStr("table"));
        assertEquals(5, json.json().getJSONArray("columns").size());
    }

    @Test
    public void execRollsBackAllDmlOnFailureAndStops() throws Exception {
        Output output = database("exec", "-s", "INSERT INTO T_USER (ID) VALUES (2); SELECT * FROM T_USER; INSERT INTO T_USER (ID) VALUES (1); INSERT INTO T_USER (ID) VALUES (3)");
        assertEquals(output.stdout, 1, output.exitCode);
        JSONObject json = output.json();
        assertFalse(json.getBool("success"));
        assertTrue(json.getBool("rolledBack"));
        assertEquals(Integer.valueOf(2), json.getInt("statementIndex"));
        assertEquals(3, json.getJSONArray("partialResults").size());
        assertEquals(1, countUsers());
    }

    @Test
    public void execCommitsAndQueriesSeeEarlierStatements() throws Exception {
        Output output = database("exec", "-s", "INSERT INTO T_USER (ID) VALUES (2); SELECT * FROM T_USER; UPDATE T_USER SET YHM='bob' WHERE ID=2");
        assertEquals(output.stdout, 0, output.exitCode);
        JSONObject json = output.json();
        assertTrue(json.getBool("success"));
        assertEquals(Integer.valueOf(2), json.getJSONArray("results").getJSONObject(1).getInt("rowCount"));
        assertEquals(2, countUsers());
    }

    @Test
    public void execAutoModeReportsFailureAndKeepsCommittedStatements() throws Exception {
        Output output = database("exec", "--mode", "auto", "-s", "INSERT INTO T_USER (ID) VALUES (2); INSERT INTO T_USER (ID) VALUES (1); INSERT INTO T_USER (ID) VALUES (3)");
        assertEquals(output.stdout, 1, output.exitCode);
        JSONObject json = output.json();
        assertFalse(json.getBool("success"));
        assertEquals("auto", json.getStr("mode"));
        assertEquals(3, json.getJSONArray("results").size());
        assertFalse(json.getJSONArray("results").getJSONObject(1).getBool("success"));
        assertTrue(json.getJSONArray("results").getJSONObject(2).getBool("success"));
        assertEquals(3, countUsers());
    }

    @Test
    public void execSqlFileRollsBackUsingSameConnection() throws Exception {
        File file = File.createTempFile("roudan-rollback-", ".sql");
        try {
            Files.write(file.toPath(), ("UPDATE T_USER SET YHM='changed' WHERE ID=1;\n"
                    + "INSERT INTO T_USER (ID) VALUES (2);\n"
                    + "DELETE FROM T_USER WHERE ID=1;\n"
                    + "INSERT INTO MISSING_TABLE VALUES (1);").getBytes(StandardCharsets.UTF_8));
            Output output = database("exec", "-f", file.getAbsolutePath());
            assertEquals(output.stdout, 1, output.exitCode);
            assertTrue(output.json().getBool("rolledBack"));
            assertEquals(1, countUsers());
            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT YHM FROM T_USER WHERE ID=1")) {
                assertTrue(rs.next());
                assertEquals("alice", rs.getString(1));
            }
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void execShowSqlDoesNotPolluteJsonOutput() throws Exception {
        Output output = database("--show-sql", "exec", "-s", "SELECT * FROM T_USER");
        assertEquals(output.stdout, 0, output.exitCode);
        assertTrue(output.json().getBool("success"));
        assertTrue(output.stderr, output.stderr.contains("SELECT * FROM T_USER"));
    }

    @Test
    public void updateStillAcceptsTargetVersionInsteadOfVersionHelp() {
        CommandLine.ParseResult result = new CommandLine(new Main()).parseArgs("update", "--version", "0.5.5");
        assertEquals("0.5.5", result.subcommand().matchedOptionValue("--version", ""));
        assertFalse(result.subcommand().isVersionHelpRequested());
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(URL, "sa", "");
    }

    private int countUsers() throws Exception {
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM T_USER")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Output database(String... args) throws Exception {
        List<String> all = new ArrayList<>(Arrays.asList("-u", URL, "-n", "sa", "-p", "", "-d", "org.h2.Driver", "-j", DRIVER_JAR));
        all.addAll(Arrays.asList(args));
        return run(all.toArray(new String[0]));
    }

    private Output run(String... args) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream out = new PrintStream(stdout, true, "UTF-8"); PrintStream err = new PrintStream(stderr, true, "UTF-8")) {
            System.setOut(out);
            System.setErr(err);
            CommandLine cli = new CommandLine(new Main())
                    .setExecutionExceptionHandler(new Main.ErrorHandler())
                    .setParameterExceptionHandler(new Main.ParamErrorHandler())
                    .setOut(new PrintWriter(out, true))
                    .setErr(new PrintWriter(err, true));
            int exit = cli.execute(args);
            return new Output(exit, stdout.toString("UTF-8"), stderr.toString("UTF-8"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static class Output {
        final int exitCode;
        final String stdout;
        final String stderr;

        Output(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        JSONObject json() {
            return JSONUtil.parseObj(stdout, JSONConfig.create().setIgnoreNullValue(false));
        }
    }
}
