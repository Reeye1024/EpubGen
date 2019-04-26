package cn.reeye;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Reeye on 2018/7/4 11:06
 * Nothing is true but improving yourself.
 */
public class EpubGen {

    @SuppressWarnings("all")
    public static void main(String[] args) throws Exception {
        String bookUrl = "https://www.bqg99.cc/book/1847953/";
//        String tempPath = "C:\\Users\\Reeye\\Desktop\\tmp\\epub";
//        String outputPath = "C:\\Users\\Reeye\\Desktop\\tmp";
        String tempPath = "/Users/reeye/Downloads/epub/tmp";
        String outputPath = "/Users/reeye/Downloads/epub";

//        if (args.length < 3) {
//            System.out.println("用法: java -jar EpubGen.jar 书籍URL 缓存目录 输出地址");
//            System.out.println("注意: 输出地址不可在缓存目录下, 且不可相同!");
//            return;
//        }
//
//        String bookUrl = args[0];
//        String tempPath = args[1];
//        String outputPath = args[2];

        Book book = new Book();

        File tempBook = new File(outputPath + File.separator + "book.temp");
        if (!tempBook.exists()) {
            Document bookDoc = Jsoup.connect(bookUrl).get();
            book.bookName = bookDoc.select("div.book>.info>h1").text();
            book.author = bookDoc.select("div.book>.info>.small>span").get(0).text();
            book.intro = bookDoc.select("div.book>.info>.intro").get(0).text();
            book.imgUrl = bookDoc.select("div.book>.info>.cover>img").get(0).attr("src");
            System.out.println("获取到书籍信息: \n" + book);

            String firstUrl = bookDoc.select("div.listmain>dl>dd").get(0).select("a").attr("href");
            while (true) {
                try {
                    Document document = Jsoup.connect(firstUrl).get();
                    Elements titleDom = document.select("div.content>h1");
                    Elements contentDom = document.select("div#content");
                    String content = contentDom.get(0).html().replaceAll("((<br\\s?>)|\n)+", "<br/>");
                    Book.Chapter chapter = new Book.Chapter(titleDom.text(), content);
                    System.out.println("获取到章节: " + chapter.title);
                    book.chapters.add(chapter);
                    String nextUrl = document.select("div.page_chapter>ul>li").get(2).select("a").attr("href");
                    if (!nextUrl.endsWith(".html")) {
                        break;
                    } else {
                        firstUrl = nextUrl;
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage() + "\n出错, 1s后继续");
                    Thread.sleep(1000L);
                }
            }
            System.out.println("章节获取完毕~");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempBook));
            oos.writeObject(book);
        } else {
            book = (Book) new ObjectInputStream(new FileInputStream(tempBook)).readObject();
        }

        File workDic = new File(tempPath);
        if (!workDic.exists()) {
            workDic.mkdirs();
        }

        //  chapter
        String chapterPath = tempPath + File.separator + "chapter";
        new File(chapterPath).mkdirs();
        String chapter_template = readFile("/res/chapter/chapter_template.xhtml");
        for (int i = 0; i < book.chapters.size(); i++) {
            String content = chapter_template.replaceAll("\\{\\{title}}", book.chapters.get(i).title)
                    .replace("{{content}}", book.chapters.get(i).content)
                    .replaceAll("\\{\\{bookName}}", book.bookName);
            writeNewFile(chapterPath + File.separator + "chapter_" + (i + 1) + ".xhtml", content);
        }

        // container.xml
        String metainfPath = tempPath + File.separator + "META-INF";
        new File(metainfPath).mkdirs();
        String container_xml = readFile("/res/META-INF/container.xml");
        writeNewFile(metainfPath + File.separator + "container.xml", container_xml);

        // catalog.xhtml
        // <li class="catalog"><a href="chapter/chapter_191573906.xhtml">第一章 书生孟浩</a></li>
        String catalog_xhtml = readFile("/res/catalog.xhtml");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < book.chapters.size(); i++) {
            sb.append("<li class=\"catalog\"><a href=\"chapter/chapter_" + (i + 1) + ".xhtml\">" + book.chapters.get(i).title + "</a></li>");
        }
        writeNewFile(tempPath + File.separator + "catalog.xhtml",
                catalog_xhtml.replaceAll("\\{\\{bookName}}", book.bookName)
                        .replaceAll("\\{\\{chapters}}", sb.toString()));

        // content.opf
        // item:   <item href="chapter/chapter_191573906.xhtml" id="id191573906" media-type="application/xhtml+xml"/>
        // itemref:     <itemref idref="id191573906"/>
        String content_opf = readFile("/res/content.opf");
        String content_opf_content = content_opf.replaceAll("\\{\\{bookName}}", book.bookName)
                .replaceAll("\\{\\{author}}", book.author)
                .replaceAll("\\{\\{time}}", Instant.now().toString());
        sb.delete(0, sb.length());
        for (int i = 0; i < book.chapters.size(); i++) {
            sb.append("<item href=\"chapter/chapter_" + (i + 1) + ".xhtml\" id=\"id" + (i + 1) + "\" media-type=\"application/xhtml+xml\"/>");
        }
        content_opf_content = content_opf_content.replaceAll("\\{\\{items}}", sb.toString());
        sb.delete(0, sb.length());
        for (int i = 0; i < book.chapters.size(); i++) {
            sb.append("<itemref idref=\"id" + (i + 1) + "\"/>");
        }
        content_opf_content = content_opf_content.replaceAll("\\{\\{itemrefs}}", sb.toString());
        writeNewFile(tempPath + File.separator + "content.opf", content_opf_content);

        // cover.jpg
        BufferedImage image = ImageIO.read(new URL(book.imgUrl));
        ImageIO.write(image, "jpg", new File(workDic + File.separator + "cover.jpg"));

        // mimetype
        String mimetype = readFile("/res/mimetype");
        writeNewFile(tempPath + File.separator + "mimetype", mimetype);

        // page.xhtml
        String page_xhtml = readFile("/res/page.xhtml");
        writeNewFile(tempPath + File.separator + "page.xhtml", page_xhtml.replaceAll("\\{\\{bookName}}", book.bookName)
                .replaceAll("\\{\\{author}}", book.author)
                .replaceAll("\\{\\{intro}}", book.intro));

        // stylesheet.css
        String stylesheet_css = readFile("/res/stylesheet.css");
        writeNewFile(tempPath + File.separator + "stylesheet.css", stylesheet_css);

        // toc.ncx
        // <navPoint id="chapter_191573906" playOrder="1"><navLabel><text>第一章 书生孟浩</text></navLabel><content src="chapter/chapter_191573906.xhtml"/></navPoint>
        String toc_ncx = readFile("/res/toc.ncx");
        String toc_ncx_content = toc_ncx.replaceAll("\\{\\{bookName}}", book.bookName)
                .replaceAll("\\{\\{author}}", book.author);
        sb.delete(0, sb.length());
        for (int i = 0; i < book.chapters.size(); i++) {
            sb.append("<navPoint id=\"chapter_" + (i + 1) + "\" playOrder=\"" + (i + 1) + "\"><navLabel><text>" + book.chapters.get(i).title + "</text></navLabel><content src=\"chapter/chapter_" + (i + 1) + ".xhtml\"/></navPoint>"
                    + System.getProperty("line.separator"));
        }
        writeNewFile(tempPath + File.separator + "toc.ncx", toc_ncx_content.replaceAll("\\{\\{navPoints}}", sb.toString()));

        // zip
        ZipCompressor zc = new ZipCompressor(outputPath + File.separator + book.bookName + ".epub");
        zc.compressAllChildren(tempPath);

        // 删除temp
        if (tempBook.exists()) {
            tempBook.delete();
        }
        delAllFiles(new File(tempPath));
    }

    @SuppressWarnings("all")
    private static void delAllFiles(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] arr = f.listFiles();
                if (arr.length > 0) {
                    for (int i = 0; i < arr.length; i++) {
                        delAllFiles(arr[i]);
                    }
                }
            }
            f.delete();
        }
    }

    private static void writeNewFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
            out.write(content);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readFile(String filePath) {
        try {
            InputStream is = EpubGen.class.getResourceAsStream(filePath);
            return new BufferedReader(new InputStreamReader(is)).lines()
                    .reduce("", (pre, curr) -> (pre.equals("") ? "" : pre + System.getProperty("line.separator")) + curr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static class Book implements Serializable {
        private String bookName;
        private String author;
        private String intro;
        private String imgUrl;
        private List<Chapter> chapters = new ArrayList<>();

        @Override
        public String toString() {
            return "Book{" +
                    "bookName='" + bookName + '\'' +
                    ", author='" + author + '\'' +
                    ", intro='" + intro + '\'' +
                    ", imgUrl='" + imgUrl + '\'' +
                    ", chapters=" + chapters +
                    '}';
        }

        static class Chapter implements Serializable {
            private String title;
            private String content;

            Chapter(String title, String content) {
                this.title = title;
                this.content = content;
            }
        }
    }

}
