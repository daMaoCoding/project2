package dc.pay.utils.excel.channelFormat;

import java.util.ArrayList;
import java.util.List;

public class LevenshteinMacthString {
    public static void main(String[] args) {
        List<String[]> list = new ArrayList<String[]>();
        list.add(new String[] { "建行宣武门支行", "发的发生的发生发生" });
        list.add(new String[] { "建行宣武门支行", "中国北京建设银行宣武门支行" });
        list.add(new String[] { "建行宣武门支行", "中国建设银行股份有限公司北京宣武门支行" });
        list.add(new String[] { "建行宣武门支行", "中国北京建设银行宣武门支行" });
        list.add(new String[] { "建行宣武门支行", "中国银行股份有限公司北京宣武门支行" });
        list.add(new String[] { "建行宣武门支行", "建行宣武门支行" });

        for (String[] a : list) {
            int cost = levenshteinMacth(a[1], a[0]);
            System.out.println(a[1] +"->"+ a[0] + "=" + cost);
        }
        System.out.println("-----------------------------------");
        for (String[] a : list) {
            int cost = levenshteinMacth(a[0], a[1]);
            System.out.println(a[0] +"->"+ a[1] + "=" + cost);
        }
    }

    public static int levenshteinMacth(String source,String target) {
        int n = target.length();
        int m = source.length();
        int[][] d = new int[n + 1][m + 1];

        // Step 1
        if (n == 0) {
            return m;
        }

        if (m == 0) {
            return n;
        }

        // Step 2
        for (int i = 0; i <= n; d[i][0] = i++) {
        }

        for (int j = 0; j <= m; d[0][j] = j++) {
        }

        // Step 3
        for (int i = 1; i <= n; i++) {
            // Step 4
            for (int j = 1; j <= m; j++) {
                // Step 5
               // System.out.println(t.charAt(j - 1));
              //  System.out.println(s.charAt(i - 1));
               // int cost = (t.charAt(j - 1) == s.charAt(i - 1)) ? 0 : 1;
                int cost = (source.substring(j - 1, j) == target.substring(i - 1, i) ? 0 : 1);

                // Step 6
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        // Step 7
        return d[n][m];
    }
}
