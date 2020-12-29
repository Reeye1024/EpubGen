package cn.reeye;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Scanner;

/**
 * Created by Reeye on 2020-09-23 16:11.
 * Nothing is true but improving yourself.
 */
public class Reader {

    public static void main(String[] args) throws Exception {
        int maxLength = 55;
        Scanner scanner = new Scanner(System.in);
        String domain = "http://m.biquge.info";
        String url = domain + "/3_3746/14830575.html";
        while (true) {
            Document document = Jsoup.connect(url).get();
            Element nr = document.getElementById("nr");
            String text = nr.html()
                    .replaceAll("(&nbsp;)|\\s", "")
                    .replaceAll("<br>", "\n")
                    .replaceAll("\n+", "\n");
            System.out.println(url);
            for (String s : text.split("\n")) {
                System.out.print("  ");
                for (int i = 0; i < s.length(); i += maxLength) {
                    int len = i + maxLength;
                    System.out.println(s.substring(i, Math.min(len, s.length())));
                }
            }
            scanner.nextLine();
            Element element = document.selectFirst("div.nr_page > a:nth-child(3)");
            if (element != null) {
                url = domain + element.attr("href");
            } else {
                System.out.println("element不存在");
                break;
            }
        }
    }

}
