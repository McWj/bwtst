import java.io.*;
import java.sql.*;
import java.util.*;

public class ExportData {
    // add by codebuddy 20260821: 读取 bwtst 全部表数据，生成可导入的 INSERT 语句
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/bwtst?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        String user = "wjdev";
        String pwd = "wjdev";
        String outFile = "bwtst_data.sql";

        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            DatabaseMetaData meta = conn.getMetaData();
            // 只导出本库的用户表（排除视图）
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables("bwtst", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            Collections.sort(tables);
            System.out.println("发现表数量: " + tables.size());

            try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {
                w.println("-- bwtst 数据导出 " + new java.util.Date());
                w.println("-- 数据库: bwtst  生成时间: " + new java.util.Date());
                w.println("SET NAMES utf8mb4;");
                w.println("SET FOREIGN_KEY_CHECKS=0;");
                w.println();

                int totalRows = 0;
                for (String table : tables) {
                    int cnt = exportTable(conn, w, table);
                    totalRows += cnt;
                    System.out.println("  " + table + " -> " + cnt + " 行");
                }

                w.println();
                w.println("SET FOREIGN_KEY_CHECKS=1;");
                System.out.println("完成，共导出 " + totalRows + " 行，输出文件: " + outFile);
            }
        }
    }

    private static int exportTable(Connection conn, PrintWriter w, String table) throws SQLException {
        // 先取列信息
        List<String> columns = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {
            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columns.add(md.getColumnName(i));
            }

            int rowCount = 0;
            StringBuilder buf = new StringBuilder();
            while (rs.next()) {
                buf.setLength(0);
                buf.append("INSERT INTO `").append(table).append("` (");
                for (int i = 0; i < colCount; i++) {
                    if (i > 0) buf.append(", ");
                    buf.append("`").append(columns.get(i)).append("`");
                }
                buf.append(") VALUES (");
                for (int i = 0; i < colCount; i++) {
                    if (i > 0) buf.append(", ");
                    Object v = rs.getObject(i + 1);
                    buf.append(formatValue(v));
                }
                buf.append(");");
                w.println(buf.toString());
                rowCount++;
            }
            if (rowCount > 0) {
                w.println();
            }
            return rowCount;
        }
    }

    private static String formatValue(Object v) {
        if (v == null) {
            return "NULL";
        }
        if (v instanceof byte[]) {
            return "0x" + bytesToHex((byte[]) v);
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        // 字符串/日期等：转义单引号与反斜杠
        String s = v.toString();
        s = s.replace("\\", "\\\\").replace("'", "''");
        // 控制字符处理
        s = s.replace("\r", "\\r").replace("\n", "\\n");
        return "'" + s + "'";
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
