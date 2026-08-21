import java.io.*;
import java.util.regex.*;

public class SplitData {
    // add by codebuddy 20260821: 将 bwtst_data.sql 按表拆分，超大表按行切块，保证每个分片 < 100MB 可推送 GitHub
    public static void main(String[] args) throws Exception {
        String in = "bwtst_data.sql";
        String outPrefix = "bwtst_data_";
        long MAX = 90L * 1024 * 1024; // 90MB 上限，留余量避免超 100MB
        Pattern p = Pattern.compile("INSERT INTO `([^`]+)`");

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), "UTF-8"));
        BufferedWriter w = null;
        String curTable = null;
        int part = 0;
        long curBytes = 0;
        String line;
        int files = 0;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("--") || line.trim().isEmpty() || line.startsWith("SET ")) {
                continue;
            }
            if (!line.startsWith("INSERT INTO")) {
                continue;
            }
            Matcher m = p.matcher(line);
            if (!m.find()) {
                continue;
            }
            String t = m.group(1);
            if (!t.equals(curTable)) {
                if (w != null) { w.flush(); w.close(); }
                curTable = t;
                part = 1;
                curBytes = 0;
                w = newWriter(outPrefix + t + "_" + String.format("%02d", part) + ".sql");
                files++;
            } else if (curBytes > MAX) {
                w.flush();
                w.close();
                part++;
                w = newWriter(outPrefix + t + "_" + String.format("%02d", part) + ".sql");
                curBytes = 0;
                files++;
            }
            w.write(line);
            w.write("\n");
            curBytes += line.getBytes("UTF-8").length + 1;
        }
        if (w != null) { w.flush(); w.close(); }
        r.close();
        System.out.println("拆分完成，生成文件数: " + files);
    }

    static BufferedWriter newWriter(String f) throws Exception {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        w.write("SET NAMES utf8mb4;\n");
        w.write("SET FOREIGN_KEY_CHECKS=0;\n\n");
        return w;
    }
}
