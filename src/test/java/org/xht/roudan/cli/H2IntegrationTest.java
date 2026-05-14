package org.xht.roudan.cli;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import org.xht.roudan.cli.command.*;
import org.xht.roudan.cli.config.CliConfig;
import org.xht.roudan.cli.config.ConfigLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class H2IntegrationTest {

    private static final String H2_JAR = System.getProperty("user.home")
            + "/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private File cwdConfigFile;

    @BeforeClass
    public static void initDataSource() throws Exception {
        Main.init(null, H2_URL, "sa", "", H2_DRIVER, H2_JAR, "default", false);
        RDConfig.setShowSql(false);
    }

    @Before
    public void setUp() throws Exception {
        RD.modify().sql("DROP ALL OBJECTS").execute();
    }

    @After
    public void tearDown() {
        if (cwdConfigFile != null && cwdConfigFile.exists()) {
            cwdConfigFile.delete();
        }
        try {
            removeEnvVar("ROUDAN_JDBC_URL");
            removeEnvVar("ROUDAN_JDBC_USER");
            removeEnvVar("ROUDAN_JDBC_PASSWORD");
            removeEnvVar("ROUDAN_JDBC_DRIVER");
            removeEnvVar("ROUDAN_JDBC_DRIVER_JAR");
        } catch (Exception ignored) {
        }
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
    public void testQueryFromFile() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT, NAME VARCHAR(64))").execute();
        RD.namedModify().sql("INSERT INTO T_USER VALUES(:id, :name)").args("id",1).args("name","张三").execute();

        QueryCommand cmd = new QueryCommand();
        setSqlFile(cmd, "sql/test_query.sql");
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
            assertEquals(Integer.valueOf(1), r.getInt("rowCount"));
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
    public void testModifyFromFile() throws Exception {
        RD.modify().sql("CREATE TABLE T_USER (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();

        ModifyCommand cmd = new ModifyCommand();
        setSqlFile(cmd, "sql/test_modify.sql");
        setNamed(cmd, true);
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

    // ==================== Day 3: Multi-datasource ====================

    @Test
    public void testMultiDatasource() throws Exception {
        String yaml =
            "datasources:\n" +
            "  db1:\n" +
            "    url: jdbc:h2:mem:testdb1;DB_CLOSE_DELAY=-1\n" +
            "    user: sa\n" +
            "    password: ''\n" +
            "    driver: org.h2.Driver\n" +
            "    driverJar: " + H2_JAR + "\n" +
            "  db2:\n" +
            "    url: jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1\n" +
            "    user: sa\n" +
            "    password: ''\n" +
            "    driver: org.h2.Driver\n" +
            "    driverJar: " + H2_JAR + "\n";

        File tmpFile = File.createTempFile("roudan-test-", ".yaml");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), yaml.getBytes());
        String configPath = tmpFile.getAbsolutePath();

        Main.init(configPath, null, null, null, null, null, "db1", false);
        RD.modify().sql("CREATE TABLE T_DB1 (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.namedModify().sql("INSERT INTO T_DB1 VALUES(:id, :name)").args("id", 1).args("name", "db1-data").execute();

        Main.init(configPath, null, null, null, null, null, "db2", false);
        RD.modify().sql("CREATE TABLE T_DB2 (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.namedModify().sql("INSERT INTO T_DB2 VALUES(:id, :name)").args("id", 1).args("name", "db2-data").execute();

        Main.init(configPath, null, null, null, null, null, "db1", false);
        List<Map<String, Object>> rows1 = RD.query().sql("SELECT * FROM T_DB1").executeQuery().result();
        assertEquals(1, rows1.size());
        assertEquals("db1-data", rows1.get(0).get("NAME"));

        Main.init(configPath, null, null, null, null, null, "db2", false);
        List<Map<String, Object>> rows2 = RD.query().sql("SELECT * FROM T_DB2").executeQuery().result();
        assertEquals(1, rows2.size());
        assertEquals("db2-data", rows2.get(0).get("NAME"));

        try {
            RD.query().sql("SELECT * FROM T_DB1").executeQuery();
            fail("Should have thrown - T_DB1 doesn't exist in db2");
        } catch (Exception e) {
        }

        Main.init(null, H2_URL, "sa", "", H2_DRIVER, H2_JAR, "default", false);
    }

    // ==================== HikariCP datasource ====================

    @Test
    public void testHikariDataSource() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(H2_URL);
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setMinimumIdle(0);
        HikariDataSource hikariDs = new HikariDataSource(hikariConfig);

        RD.dataSourceConfig(c -> c.addDataSource(hikariDs));
        RD.modify().sql("CREATE TABLE T_HIKARI (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.namedModify().sql("INSERT INTO T_HIKARI VALUES(:id, :name)").args("id", 1).args("name", "hikari-test").execute();

        List<Map<String, Object>> rows = RD.query().sql("SELECT * FROM T_HIKARI").executeQuery().result();
        assertEquals(1, rows.size());
        assertEquals("hikari-test", rows.get(0).get("NAME"));

        hikariDs.close();
        Main.init(null, H2_URL, "sa", "", H2_DRIVER, H2_JAR, "default", false);
    }

    // ==================== Config auto-discovery ====================

    @Test
    public void testEnvUrlDiscovery() throws Exception {
        setEnvVar("ROUDAN_JDBC_URL", H2_URL);
        setEnvVar("ROUDAN_JDBC_DRIVER", H2_DRIVER);
        setEnvVar("ROUDAN_JDBC_DRIVER_JAR", H2_JAR);
        CliConfig config = ConfigLoader.load(null, null, null, null, null, null);
        assertEquals(H2_URL, config.getUrl());
        assertEquals(H2_DRIVER, config.getDriverClass());
        assertEquals(H2_JAR, config.getDriverJar());
    }

    @Test
    public void testEnvUserDiscovery() throws Exception {
        setEnvVar("ROUDAN_JDBC_URL", H2_URL);
        setEnvVar("ROUDAN_JDBC_USER", "envuser");
        setEnvVar("ROUDAN_JDBC_DRIVER", H2_DRIVER);
        setEnvVar("ROUDAN_JDBC_DRIVER_JAR", H2_JAR);
        CliConfig config = ConfigLoader.load(null, null, null, null, null, null);
        assertEquals(H2_URL, config.getUrl());
        assertEquals("envuser", config.getUser());
    }

    @Test
    public void testCwdConfigAutoDiscovery() throws Exception {
        File existing = new File("roudan-config.yaml").getAbsoluteFile();
        File backup = null;
        if (existing.exists()) {
            backup = new File(existing.getParent(), "roudan-config.yaml.bak");
            existing.renameTo(backup);
        }
        String yaml =
            "datasources:\n" +
            "  default:\n" +
            "    url: " + H2_URL + "\n" +
            "    user: cwduser\n" +
            "    password: ''\n" +
            "    driver: " + H2_DRIVER + "\n" +
            "    driverJar: " + H2_JAR + "\n";
        cwdConfigFile = new File("roudan-config.yaml").getAbsoluteFile();
        Files.write(cwdConfigFile.toPath(), yaml.getBytes());
        assertTrue("CWD config file should exist", cwdConfigFile.exists());
        try {
            CliConfig config = ConfigLoader.load(null, null, null, null, null, null);
            assertEquals(H2_URL, config.getUrl());
            assertEquals("cwduser", config.getUser());
            assertEquals(H2_DRIVER, config.getDriverClass());
        } finally {
            if (cwdConfigFile.exists()) {
                cwdConfigFile.delete();
            }
            if (backup != null && backup.exists()) {
                backup.renameTo(existing);
            }
            cwdConfigFile = null;
        }
    }

    // ==================== Week 3.1: Connection timeout ====================

    @Test
    public void testConnectTimeout() throws Exception {
        Main tm = createMain();
        tm.setConnectTimeout(30000);

        TestCommand cmd = new TestCommand();
        setParent(cmd, tm);
        assertCmd(cmd, r -> {
            assertTrue(r.getBool("success"));
        });
    }

    @Test
    public void testConnectTimeoutDefault() {
        Main m = new Main();
        assertEquals(30000, m.getConnectTimeout());
    }

    // ==================== Week 3.2: Dry-run mode ====================

    @Test
    public void testDryRunQuery() throws Exception {
        RD.modify().sql("CREATE TABLE T_DRY (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();
        RD.namedModify().sql("INSERT INTO T_DRY VALUES(:id, :name)").args("id", 1).args("name", "dry").execute();

        Main tm = createMain();
        tm.setDryRun(true);

        QueryCommand cmd = new QueryCommand();
        setSql(cmd, "SELECT * FROM T_DRY");
        setParent(cmd, tm);
        JSONObject r = execCmdJson(cmd);
        assertTrue(r.getBool("success"));
        assertTrue(r.getBool("dryRun"));
        assertEquals("SELECT * FROM T_DRY", r.getStr("sql"));
        assertEquals(Integer.valueOf(0), r.getInt("timeMs"));
        assertNull(r.get("rows"));
    }

    @Test
    public void testDryRunModify() throws Exception {
        RD.modify().sql("CREATE TABLE T_DRYMOD (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();

        Main tm = createMain();
        tm.setDryRun(true);

        ModifyCommand cmd = new ModifyCommand();
        setSql(cmd, "INSERT INTO T_DRYMOD VALUES(1, 'dry')");
        setParent(cmd, tm);
        JSONObject r = execCmdJson(cmd);
        assertTrue(r.getBool("success"));
        assertTrue(r.getBool("dryRun"));
        assertEquals("INSERT INTO T_DRYMOD VALUES(1, 'dry')", r.getStr("sql"));

        List<Map<String, Object>> rows = RD.query().sql("SELECT * FROM T_DRYMOD").executeQuery().result();
        assertEquals(0, rows.size());
    }

    @Test
    public void testDryRunCount() throws Exception {
        RD.modify().sql("CREATE TABLE T_DRYCNT (ID INT, VAL INT)").execute();
        RD.modify().sql("INSERT INTO T_DRYCNT VALUES(1, 10)").execute();
        RD.modify().sql("INSERT INTO T_DRYCNT VALUES(2, 20)").execute();

        Main tm = createMain();
        tm.setDryRun(true);

        CountCommand cmd = new CountCommand();
        setSql(cmd, "SELECT * FROM T_DRYCNT");
        setParent(cmd, tm);
        JSONObject r = execCmdJson(cmd);
        assertTrue(r.getBool("success"));
        assertTrue(r.getBool("dryRun"));
        assertEquals("SELECT * FROM T_DRYCNT", r.getStr("sql"));
    }

    // ==================== Week 3.3: Transaction commands ====================

    @Test
    public void testTransactionRollback() throws Exception {
        RD.modify().sql("CREATE TABLE T_TX1 (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();

        Main tm = createMain();

        BeginCommand begin = new BeginCommand();
        setParent(begin, tm);
        JSONObject br = execCmdJson(begin);
        assertTrue(br.getBool("success"));

        ModifyCommand modify = new ModifyCommand();
        setSql(modify, "INSERT INTO T_TX1 VALUES(:id, :name)");
        setNamed(modify, true);
        setModifyArgs(modify, "{\"id\":1,\"name\":\"tx-test\"}");
        setParent(modify, tm);
        JSONObject mr = execCmdJson(modify);
        assertTrue(mr.getBool("success"));
        assertEquals(Integer.valueOf(1), mr.getInt("affectedRows"));

        RollbackCommand rollback = new RollbackCommand();
        setParent(rollback, tm);
        JSONObject rr = execCmdJson(rollback);
        assertTrue(rr.getBool("success"));

        Main.clearTxConnection();
        QueryCommand query = new QueryCommand();
        setSql(query, "SELECT * FROM T_TX1");
        setParent(query, tm);
        assertCmd(query, r -> {
            assertEquals(Integer.valueOf(0), r.getInt("rowCount"));
        });
    }

    @Test
    public void testTransactionCommit() throws Exception {
        RD.modify().sql("CREATE TABLE T_TX2 (ID INT PRIMARY KEY, NAME VARCHAR(64))").execute();

        Main tm = createMain();

        BeginCommand begin = new BeginCommand();
        setParent(begin, tm);
        JSONObject br = execCmdJson(begin);
        assertTrue(br.getBool("success"));

        ModifyCommand modify = new ModifyCommand();
        setSql(modify, "INSERT INTO T_TX2 VALUES(:id, :name)");
        setNamed(modify, true);
        setModifyArgs(modify, "{\"id\":1,\"name\":\"tx-commit\"}");
        setParent(modify, tm);
        JSONObject mr = execCmdJson(modify);
        assertTrue(mr.getBool("success"));
        assertEquals(Integer.valueOf(1), mr.getInt("affectedRows"));

        CommitCommand commit = new CommitCommand();
        setParent(commit, tm);
        JSONObject cr = execCmdJson(commit);
        assertTrue(cr.getBool("success"));

        Main.clearTxConnection();
        QueryCommand query = new QueryCommand();
        setSql(query, "SELECT * FROM T_TX2");
        setParent(query, tm);
        assertCmd(query, r -> {
            assertEquals(Integer.valueOf(1), r.getInt("rowCount"));
        });
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

    private void setParent(Object cmd, Main m) throws Exception {
        fieldOf(cmd, "main").set(cmd, m);
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
    private void setSqlFile(Object cmd, String resourcePath) throws Exception {
        String path = new java.io.File(getClass().getClassLoader().getResource(resourcePath).toURI()).getAbsolutePath();
        fieldOf(cmd, "sqlFile").set(cmd, path);
    }

    @SuppressWarnings("unchecked")
    private JSONObject execCmdJson(Object cmd) throws Exception {
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
            throw new RuntimeException("Command returned exit code " + exit + ". Output: " + output);
        }
        return JSONUtil.parseObj(output);
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

    // ==================== env var helpers ====================

    @SuppressWarnings("unchecked")
    private static void setEnvVar(String key, String value) throws Exception {
        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
        java.lang.reflect.Field theEnvironmentField = processEnvironment.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.put(key, value);
        java.lang.reflect.Field ciEnvField = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
        ciEnvField.setAccessible(true);
        Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
        ciEnv.put(key, value);
    }

    @SuppressWarnings("unchecked")
    private static void removeEnvVar(String key) throws Exception {
        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
        java.lang.reflect.Field theEnvironmentField = processEnvironment.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.remove(key);
        java.lang.reflect.Field ciEnvField = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
        ciEnvField.setAccessible(true);
        Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
        ciEnv.remove(key);
    }
}
