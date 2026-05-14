package org.xht.roudan.cli;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import org.xht.roudan.cli.command.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class H2ModeIntegrationTest {

    private static final String H2_JAR = System.getProperty("user.home")
            + "/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String MYSQL_URL = "jdbc:h2:mem:mysqltest;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String PGSQL_URL = "jdbc:h2:mem:pgsqltest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    private String currentUrl;
    private String currentName;

    @BeforeClass
    public static void initDataSources() throws Exception {
        Main.init(null, MYSQL_URL, "sa", "", H2_DRIVER, H2_JAR, "mysql", false);
        Main.init(null, PGSQL_URL, "sa", "", H2_DRIVER, H2_JAR, "pgsql", false);
        RDConfig.setShowSql(false);
    }

    @Before
    public void setUp() throws Exception {
        RD.datasource("mysql");
        RD.modify().sql("DROP ALL OBJECTS").execute();
        RD.datasource("pgsql");
        RD.modify().sql("DROP ALL OBJECTS").execute();
    }

    private void switchTo(String name, String url) {
        this.currentName = name;
        this.currentUrl = url;
        RD.datasource(name);
    }

    // ==================== MySQL mode ====================

    @Test
    public void testMysqlConnection() throws Exception {
        switchTo("mysql", MYSQL_URL);
        assertCmd(new TestCommand(), r -> {
            assertTrue(r.getBool("success"));
            assertTrue(r.getStr("dbProduct").startsWith("H2"));
        });
    }

    @Test
    public void testMysqlAutoIncrement() throws Exception {
        switchTo("mysql", MYSQL_URL);
        RD.modify().sql("CREATE TABLE T_USER (ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME) VALUES ('Alice')").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME) VALUES ('Bob')").execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(2), r.getInt("rowCount"));
        });
    }

    @Test
    public void testMysqlCount() throws Exception {
        switchTo("mysql", MYSQL_URL);
        RD.modify().sql("CREATE TABLE T_USER (ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR(64), AGE INT)").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME, AGE) VALUES ('A', 20)").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME, AGE) VALUES ('B', 30)").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME, AGE) VALUES ('C', 30)").execute();

        CountCommand cmd = new CountCommand();
        setSql(cmd, "SELECT * FROM T_USER WHERE AGE > ?");
        setArgs(cmd, "[20]");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Long.valueOf(2), r.getLong("count"));
        });
    }

    // ==================== PostgreSQL mode ====================

    @Test
    public void testPgsqlConnection() throws Exception {
        switchTo("pgsql", PGSQL_URL);
        assertCmd(new TestCommand(), r -> {
            assertTrue(r.getBool("success"));
            assertTrue(r.getStr("dbProduct").startsWith("H2"));
        });
    }

    @Test
    public void testPgsqlSerial() throws Exception {
        switchTo("pgsql", PGSQL_URL);
        RD.modify().sql("CREATE TABLE T_USER (ID SERIAL PRIMARY KEY, NAME VARCHAR(64), ACTIVE BOOLEAN)").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME, ACTIVE) VALUES ('Alice', TRUE)").execute();
        RD.modify().sql("INSERT INTO T_USER (NAME, ACTIVE) VALUES ('Bob', FALSE)").execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(2), r.getInt("rowCount"));
        });
    }

    @Test
    public void testPgsqlBooleanCount() throws Exception {
        switchTo("pgsql", PGSQL_URL);
        RD.modify().sql("CREATE TABLE T_ITEM (ID SERIAL PRIMARY KEY, NAME VARCHAR(64), ACTIVE BOOLEAN)").execute();
        RD.modify().sql("INSERT INTO T_ITEM (NAME, ACTIVE) VALUES ('X', TRUE)").execute();
        RD.modify().sql("INSERT INTO T_ITEM (NAME, ACTIVE) VALUES ('Y', FALSE)").execute();
        RD.modify().sql("INSERT INTO T_ITEM (NAME, ACTIVE) VALUES ('Z', TRUE)").execute();

        CountCommand cmd = new CountCommand();
        setSql(cmd, "SELECT * FROM T_ITEM WHERE ACTIVE = :active");
        setNamed(cmd, true);
        setArgs(cmd, "{\"active\":true}");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Long.valueOf(2), r.getLong("count"));
        });
    }

    // ==================== assert helpers ====================

    interface JsonAsserter {
        void check(JSONObject r) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private void assertCmd(Object cmd, JsonAsserter asserter) throws Exception {
        setParent(cmd);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(buf));
        int exit;
        try {
            exit = ((java.util.concurrent.Callable<Integer>) cmd).call();
        } finally {
            System.setOut(old);
        }
        String output = buf.toString().trim();
        if (exit != 0) {
            fail("Command returned exit code " + exit + ". Output: " + output);
        }
        if (output.isEmpty()) {
            fail("No stdout output from command");
        }
        JSONObject r = JSONUtil.parseObj(output);
        asserter.check(r);
    }

    @SuppressWarnings("unchecked")
    private void assertCmdError(Object cmd, String expectedErrorCode) throws Exception {
        setParent(cmd);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(buf));
        int exit;
        try {
            exit = ((java.util.concurrent.Callable<Integer>) cmd).call();
        } finally {
            System.setOut(old);
        }
        String output = buf.toString().trim();
        assertEquals("Expected exit code 1, got " + exit + ". Output: " + output, 1, exit);
        JSONObject r = JSONUtil.parseObj(output);
        assertFalse(r.getBool("success"));
        assertEquals(expectedErrorCode, r.getStr("errorCode"));
    }

    // ==================== reflection helpers ====================

    private java.lang.reflect.Field fieldOf(Object cmd, String name) throws Exception {
        java.lang.reflect.Field f = cmd.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private void setParent(Object cmd) throws Exception {
        fieldOf(cmd, "main").set(cmd, createMain());
    }

    private void setSql(Object cmd, String sql) throws Exception {
        fieldOf(cmd, "sql").set(cmd, sql);
    }

    private void setArgs(Object cmd, String args) throws Exception {
        fieldOf(cmd, "argsJson").set(cmd, args);
    }

    private void setNamed(Object cmd, boolean named) throws Exception {
        fieldOf(cmd, "named").set(cmd, named);
    }

    private void setTable(Object cmd, String table) throws Exception {
        fieldOf(cmd, "tableName").set(cmd, table);
    }

    private Main createMain() {
        Main m = new Main();
        m.setConfigFile(null);
        m.setJdbcUrl(currentUrl);
        m.setUser("sa");
        m.setPassword("");
        m.setDriverClass(H2_DRIVER);
        m.setDriverJar(H2_JAR);
        m.setDatasourceName(currentName);
        m.setOutputFormat("json");
        m.setPretty(false);
        m.setShowSql(false);
        return m;
    }
}
