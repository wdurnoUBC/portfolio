package sqlInterface;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Test;

public class SQLTest {

	@Test
	public void test() {
		SQL2.createTable("TestTable");
		SQL2.addColumn("TestTable", "col1");
		Vector<String> values = new Vector<String>();
		values.add("col1");
		
		assertEquals(SQL2.insert("TestTable", values), "1");
		Vector<Vector<String>> result = SQL2.selectAll("TestTable", false);
		System.out.println(result);
		
//		SQL1.removeTable("TestTable");
	}

}
