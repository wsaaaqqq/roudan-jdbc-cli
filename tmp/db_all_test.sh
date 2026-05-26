#!/bin/bash
set -e
RD="java -jar /mnt/c/ws/java/roudan-jdbc-cli/target/roudan-jdbc-cli.jar"

# Driver JARs (Windows paths → WSL paths via /mnt/c)
MYSQL_JAR="/mnt/c/Users/xht/.m2/repository/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"
PG_JAR="/mnt/c/Users/xht/.m2/repository/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar"
MARIA_JAR="/mnt/c/Users/xht/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.3.2/mariadb-java-client-3.3.2.jar"
DM7_JAR="/mnt/c/Users/xht/.m2/repository/com/dameng/DmJdbcDriver7/8.1.4.181/DmJdbcDriver7-8.1.4.181.jar"

echo "=== DB TEST RESULTS ==="

# Start containers
docker rm -f mysql8 pg15 mariadb11 yth-dm7 2>/dev/null || true

docker run -d --name mysql8 -e MYSQL_ROOT_PASSWORD=root -p 33060:3306 mysql:8.0
docker run -d --name pg15 -e POSTGRES_PASSWORD=postgres -p 54320:5432 postgres:15
docker run -d --name mariadb11 -e MARIADB_ROOT_PASSWORD=root -p 33070:3306 mariadb:11
docker run -d -p 5236:5236 --name yth-dm7 yth-dm7:latest

# Wait functions
wait_mysql() { for i in $(seq 1 60); do docker exec mysql8 mysqladmin ping -h localhost --silent 2>/dev/null && return 0; sleep 2; done; return 1; }
wait_pg() { for i in $(seq 1 30); do docker exec pg15 pg_isready -U postgres 2>/dev/null && return 0; sleep 2; done; return 1; }
wait_mariadb() { for i in $(seq 1 30); do docker exec mariadb11 mysqladmin ping -h localhost --silent 2>/dev/null && return 0; sleep 2; done; return 1; }
wait_dm7() { sleep 25; return 0; }

echo "Waiting for databases..."
wait_mysql && echo "MySQL ready" || echo "MySQL timeout"
wait_pg && echo "PostgreSQL ready" || echo "PG timeout"
wait_mariadb && echo "MariaDB ready" || echo "MariaDB timeout"
wait_dm7 && echo "DM7 ready" || echo "DM7 timeout"

# D3: MySQL Tests
echo "--- D3 MySQL ---"
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" test 2>&1 | grep -o '{.*}'
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" modify -s "CREATE TABLE IF NOT EXISTS t_test (id INT PRIMARY KEY, name VARCHAR(50))" 2>&1
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" modify -s "INSERT INTO t_test VALUES (1, 'Alice'), (2, 'Bob')" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" query -s "SELECT * FROM t_test" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" count -s "SELECT COUNT(*) FROM t_test" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" tables 2>&1 | grep -o '{.*}'
$RD -u jdbc:mysql://localhost:33060/mysql -n root -p root -d com.mysql.cj.jdbc.Driver -j "$MYSQL_JAR" describe -t t_test 2>&1 | grep -o '{.*}'

# D4: PostgreSQL Tests
echo "--- D4 PostgreSQL ---"
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" test 2>&1 | grep -o '{.*}'
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" modify -s "CREATE TABLE t_pgtest (id INT PRIMARY KEY, name VARCHAR(50))" 2>&1 | grep -o '{.*}'
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" modify -s "INSERT INTO t_pgtest VALUES (1, 'Bob')" 2>&1 | grep -o '{.*}'
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" query -s "SELECT * FROM t_pgtest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" count -s "SELECT COUNT(*) FROM t_pgtest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:postgresql://localhost:54320/postgres -n postgres -p postgres -d org.postgresql.Driver -j "$PG_JAR" describe -t t_pgtest 2>&1 | grep -o '{.*}'

# D6: MariaDB Tests
echo "--- D6 MariaDB ---"
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" test 2>&1 | grep -o '{.*}'
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" modify -s "CREATE TABLE t_matest (id INT PRIMARY KEY, name VARCHAR(50))" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" modify -s "INSERT INTO t_matest VALUES (1, 'Charlie')" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" query -s "SELECT * FROM t_matest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" count -s "SELECT COUNT(*) FROM t_matest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:mariadb://localhost:33070/mysql -n root -p root -d org.mariadb.jdbc.Driver -j "$MARIA_JAR" describe -t t_matest 2>&1 | grep -o '{.*}'

# D10: DM7 Tests
echo "--- D10 DM7 ---"
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" test 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" modify -s "CREATE TABLE t_dmtest (id INT PRIMARY KEY, name VARCHAR(50))" 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" modify -s "INSERT INTO t_dmtest VALUES (1, 'DM Test')" 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" query -s "SELECT * FROM t_dmtest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" count -s "SELECT COUNT(*) FROM t_dmtest" 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" tables 2>&1 | grep -o '{.*}'
$RD -u jdbc:dm://localhost:5236 -n SYSDBA -p SYSDBA -d dm.jdbc.driver.DmDriver -j "$DM7_JAR" describe -t t_dmtest 2>&1 | grep -o '{.*}'

echo "=== DB TEST RESULTS END ==="
