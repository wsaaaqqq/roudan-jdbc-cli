package org.xht.roudan.cli;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import org.xht.roudan.cli.command.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class H2IntegrationTest {

    private static final String H2_JAR = System.getProperty("user.home")
            + "/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

    @BeforeClass
    public static void initDataSource() throws Exception {
        Main.init(null, H2_URL, "sa", "", H2_DRIVER, H2_JAR, "default", false);
        RDConfig.setShowSql(false);
    }

    @Before
    public void setUp() throws Exception {
        RD.modify().sql("DROP ALL OBJECTS").execute();
    }

    @Test
    public void testConnection() throws Exception {
        assertCmd(new TestCommand(), r -> {
            assertTrue(r.getBool("success"));
            assertTrue(r.getStr("dbProduct").startsWith("H2"));
        });
    }

    @Test
    public void testTables() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.modify().sql("CREATE VIEW V_USER AS SELECT * FROM T_USER").execute();

        TablesCommand cmd = new TablesCommand();
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            JSONArray tables = r.getJSONArray("tables");
            assertTrue(tables.size() >= 2);
        });
    }

    @Test
    public void testDescribe() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT PRIMARY KEY, NAME VARCHAR(64), AGE INT, SALARY DECIMAL(10,2))").execute();

        DescribeCommand cmd = new DescribeCommand();
        setTable(cmd, "T_USER");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals("T_USER", r.getStr("table"));
            JSONArray cols = r.getJSONArray("columns");
            assertEquals(Integer.valueOf(4), Integer.valueOf(cols.size()));
            JSONObject idCol = cols.getJSONObject(0);
            assertTrue(idCol.getBool("pk"));
        });
    }

    @Test
    public void testQueryNoArgs() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64), AGE INT)").execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",1).args("name","张三").args("age",25).execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",2).args("name","李四").args("age",30).execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(2), r.getInt("rowCount"));
            Object ageVal = r.getJSONArray("rows").getJSONArray(0).get(2);
            assertTrue("AGE should be Number", ageVal instanceof Number);
        });
    }

    @Test
    public void testQueryPositionalArgs() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64), AGE INT)").execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",1).args("name","张三").args("age",25).execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",2).args("name","李四").args("age",30).execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER WHERE age > ?");
        setArgs(cmd, "[25]");
        assertCmd(cmd, r -> {
            assertEquals(Integer.valueOf(1), r.getInt("rowCount"));
        });
    }

    @Test
    public void testQueryNamedArgs() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64), AGE INT)").execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",1).args("name","张三").args("age",25).execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",2).args("name","李四").args("age",30).execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER WHERE age > :minAge");
        setNamed(cmd, true);
        setArgs(cmd, "{\"minAge\":25}");
        assertCmd(cmd, r -> {
            assertEquals(Integer.valueOf(1), r.getInt("rowCount"));
        });
    }

    @Test
    public void testQueryLimit() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64))").execute();
        for (int i = 1; i <= 5; i++)
            RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name)").args("id",i).args("name","U"+i).execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER");
        setLimit(cmd, 2);
        assertCmd(cmd, r -> {
            assertEquals(Integer.valueOf(5), r.getInt("rowCount"));
            assertEquals(Integer.valueOf(2), Integer.valueOf(r.getJSONArray("rows").size()));
        });
    }

    @Test
    public void testQueryEmptyResult() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64))").execute();

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_USER WHERE ID=999");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(0), r.getInt("rowCount"));
        });
    }

    @Test
    public void testModifyInsert() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();

        ModifyCommand cmd = new ModifyCommand();
        setSql(cmd, "INSERT INTO T_USER VALUES(:id, :name)");
        setModifyArgs(cmd, "{\"id\":1,\"name\":\"王五\"}");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(1), r.getInt("affectedRows"));
        });
    }

    @Test
    public void testModifyDDL() throws Exception {
        ModifyCommand cmd = new ModifyCommand();
        setSql(cmd, "CREATE TABLE T_LOG (ID VARCHAR(32), MSG TEXT)");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(0), r.getInt("affectedRows"));
        });
    }

    @Test
    public void testCount() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64), AGE INT)").execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",1).args("name","A").args("age",20).execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name, :age)").args("id",2).args("name","B").args("age",30).execute();

        CountCommand cmd = new CountCommand();
        setSql(cmd, "SELECT * FROM T_USER WHERE age > ?");
        setArgs(cmd, "[20]");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Long.valueOf(1), r.getLong("count"));
        });
    }

    // ==================== Day 2: Error handling ====================

    @Test
    public void testSQLError() throws Exception {
        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM NONEXISTENT_TABLE");
        assertCmdError(cmd, "SQL_ERROR");
    }

    @Test
    public void testQueryNoArgsWhenMissing() throws Exception {
        QueryCommand cmd = new QueryCommand();
        setParent(cmd);
        int exit = cmd.call();
        assertEquals(1, exit);
    }

    @Test
    public void testModifyInsertNoArgs() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        ModifyCommand cmd = new ModifyCommand();
        setSql(cmd, "INSERT INTO T_USER VALUES(:id, :name)");
        setModifyArgs(cmd, "{}");
        assertCmdError(cmd, "SQL_ERROR");
    }

    // ==================== assert helper ====================

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
    private void setModifyArgs(Object cmd, String args) throws Exception {
        fieldOf(cmd, "argsJson").set(cmd, args);
    }
    private void setNamed(Object cmd, boolean named) throws Exception {
        fieldOf(cmd, "named").set(cmd, named);
    }
    private void setLimit(Object cmd, int limit) throws Exception {
        fieldOf(cmd, "limit").set(cmd, limit);
    }
    private void setTable(Object cmd, String table) throws Exception {
        fieldOf(cmd, "tableName").set(cmd, table);
    }

    private Main createMain() {
        Main m = new Main();
        m.setConfigFile(null);
        m.setJdbcUrl(H2_URL);
        m.setUser("sa");
        m.setPassword("");
        m.setDriverClass(H2_DRIVER);
        m.setDriverJar(H2_JAR);
        m.setDatasourceName("default");
        m.setOutputFormat("json");
        m.setPretty(false);
        m.setShowSql(false);
        return m;
    }
}
