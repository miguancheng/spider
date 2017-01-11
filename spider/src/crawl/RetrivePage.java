package crawl;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;

public class RetrivePage {

	private static CloseableHttpClient httpclient = HttpClients.createDefault(); // 创建一个客户端
	private static String filename = "D:" + File.separator + File.separator + "result.html"; // 定义输出html文件的路径
	private static String outfile = "D:" + File.separator + File.separator + "result.csv"; // 定义输出csv文件的路径
	private static boolean bfile = true; // 定义控制输出file的boolean变量
	private static ArrayList<String> datalist = new ArrayList<String>(); // 定义Arraylist类集用来保存每一条数据的信息
	private static String headtitle = "序号,书籍名称,评分,评价人数,作者,出版社,出版日期,价格"; // 打印的标题头
	private static int countrs = 0; // 计数变量

	/**
	 * 下载页面
	 */
	public static String downloadPage(String url) throws Exception {

		String htmlString = ""; // 定义返回的String变量
		HttpGet request = new HttpGet(url); // 请求资源

		CloseableHttpResponse response = httpclient.execute(request); // 得到回应

		try {

			System.out.println("状态码为：" + response.getStatusLine()); // 打印状态码

			HttpEntity entity = response.getEntity(); // 获得Entity对象
			htmlString = EntityUtils.toString(entity); // 将Entity对象转化为字符串
			EntityUtils.consume(entity); // 销毁对象
		} finally {
			response.close();
		}
		htmltoFile(htmlString); // 调用htmltoFile()方法在制定路径输出html文件

		return htmlString;

	}

	/**
	 * 输出html文件
	 */
	public static void htmltoFile(String htmlString) throws Exception {
		// 获得文件输出流
		FileOutputStream output = new FileOutputStream(filename);
		// 以utf-8编码的形式输出到文件（utf-8是中文编码，ISO-8859-1是英文编码）
		output.write(htmlString.getBytes("utf-8"));
		if (output != null) {
			output.close();
		}
	}

	/**
	 * 获取所有豆瓣编程图书列表
	 * 
	 * @throws Exception
	 */
	public static void getDouBanList(String surl) throws Exception {

		String html = RetrivePage.downloadPage(surl); // 通过url下载页面

		html = html.replace("star clearfix", "star_clearfix"); // 用"star_clearfix"替代"star clearfix"

		Document doc = Jsoup.parse(html); // 解析获取Document对象
		Element ulNode = doc.getElementsByClass("subject-list").first(); // 通过getElementsByClass方法获取class为"subject-list"的ul节点对象
		Elements liTag = ulNode.select("li[class]"); // 通过select选择器选择有class属性的li标签节点，返回Element元素的集合
		String title,mark,amount,writer,publishing,date,price;//书名,评分,评分人数,作者,出版社,出版日期,价格

		for (Element liNode : liTag) { // 对于liTag Element集合中的每一个元素liNode
			datalist.clear(); // 添加每一条数据前先清空之前添加的内容（由于是循环添加，一定要清空前一次添加的内容）
			
			Element h2 = liNode.select("h2").first(); // 取得liNode的第一个h2节点对象
			title = h2.getElementsByTag("a").text(); // 使用getElementsByTag方法，通过标签名称取得a标签节点对象，然后取其中的文本元素，即为书籍名称
			datalist.add(title); // 将title(书籍名称)添加进datalist集合
			
			Elements divMark = liNode.select("div");  //获得li下的所有div对象
			Element markElement = divMark.get(3); //第四个div为有关评价的对象
			Elements spanElement = markElement.select("span");  //获得所有span对象
			if(spanElement.size() == 1){
				continue;
			}
			Element emark = spanElement.get(1);
			Element eamount =spanElement.get(2);
			mark = emark.text();  //选择评价信息的div节点对象中的第二个span节点对象，取它的文本内容作为评价分数
			datalist.add(mark);//将mark(评价分数)添加进datalist集合
			amount = eamount.text();  //选择评价信息的div节点对象中的第三个span节点对象，取它的文本内容作为评价人数
			if(Integer.valueOf(amount.substring(1,amount.length()-4)) < 1000){
				continue;
			}
			datalist.add(amount.substring(1,amount.length()-4)); //将评价人数(amount)添加进datalist集合
			
			Elements divInfo = liNode.select("div");  //获得书籍作者,出版社,出版日期,价格信息的div节点对象
			String divInfoStr = divInfo.get(2).text();  //获得书籍作者,出版社,出版日期,价格信息的文本
			String[] divInfoArray = divInfoStr.split("/"); //将书籍作者,出版社,出版日期,价格信息的文本转换为数组
			int length = divInfoArray.length;
			price = divInfoArray[length-1].trim();  //获得书籍的价格
			date = divInfoArray[length-2].trim();  //获得书籍的出版日期
			publishing = divInfoArray[length-3].trim();  //获得书籍的出版日期
			writer = divInfoArray[0].trim();
			datalist.add(writer);
			datalist.add(publishing);
			datalist.add(date);
			datalist.add(price);
			outputRs();   //调用outputRs方法将datalist里面的每一条数据插入到表格中
		}

	}

	/**
	 * 输出到csv文件中
	 * 
	 * @throws Exception
	 */
	private static void outputRs() throws Exception {

		String strout = "";
		datalist.add(0,String.valueOf(countrs+1));
		
		for (int i = 0; i < datalist.size(); i++) {
			strout = strout + datalist.get(i) + ","; // 获取datalist集合中的每一条数据，串成一个字符串
		}

		if (bfile && countrs < 40) {
			FileWriter fw = new FileWriter(outfile, true); // 实例化文件输出流
			PrintWriter out = new PrintWriter(fw); // 实例化打印流
			if (countrs == 0)
				out.println(headtitle); // 输出头标题
			out.println(strout); // 输出刚刚串起来的strout字符串
			out.close(); // 关闭打印流
			fw.close(); // 关闭输出流
			countrs = countrs + 1;
			System.out.println(countrs + "  " + strout); // 在命令行打印数据
		}
		
	}

	/**
	 * 翻页爬取
	 * 
	 * @throws Exception
	 */
	public static void skipPage(String surl) throws Exception {
		String html = RetrivePage.downloadPage(surl);
		Document doc = Jsoup.parse(html);
		Element footDiv = doc.getElementsByClass("paginator").first(); // 获取页码部分的div对象
		Element footSpan = footDiv.getElementsByClass("next").first(); // 获取class为"next"的节点对象用footSpan表示
		Element footA = footSpan.select("a[href]").first(); // 选择footSpan中第一个带有href属性的a节点对象，并用footA表示
		String href = footA.attr("href"); // 获得footA中href属性中的内容href
		String http = "https://book.douban.com" + href; // 将"https://book.douban.com"和href拼接即为下一页面的url
		Element thispage = doc.getElementsByClass("thispage").first(); // 获取当前页码节点
		int end = Integer.parseInt(thispage.text()); // 获取当前页码中的数字元素（String类型），并转化为int类型
		if (end == 1) {
			getDouBanList(surl);
			System.out.println("==========================" + 1 + "===================");
		}
		getDouBanList(http); // 爬取下一页面
		System.out.println("==========================" + (end + 1) + "==================="); // 打印一行页面分隔符
		if (end < 20) {
			skipPage(http); // 爬取20页
		} else {
			System.out.println("页面已爬完");
		}
	}

	/**
	 * 测试代码
	 */
	public static void main(String[] args) {

		String strURL = "https://book.douban.com/tag/%E7%BC%96%E7%A8%8B";
		try {
			skipPage(strURL); // 翻页爬取
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
