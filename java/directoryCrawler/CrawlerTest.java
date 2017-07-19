package directoryCrawler;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.Test;

public class CrawlerTest {
	
	private Crawler myCrawler;
	private File root;

	@Test
	public void test() {
		root = new File("/Users/frussell/Desktop/Website content/Banner");
		
		myCrawler = new Crawler(root);
		try {
			myCrawler.initialCrawl();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		assertTrue(myCrawler.isDirectory(root));
		
		Set<File> files = myCrawler.getFiles();
		for (File f : files) {
			System.out.println(myCrawler.getName(f));
			System.out.println(myCrawler.isDirectory(f));
			System.out.println(myCrawler.getSub(f));
		}
		System.out.println(files.size());
		
	}

}
