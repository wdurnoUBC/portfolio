package sqlInterface;

import hallam.microbiology.ubc.util.JDBCUtilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Set;
import java.util.Vector;

public class SQL2 {

	private static JDBCUtilities util;

	public static int META_LIMS_TABLE_1 = 1+3 ; // Table size
	public static int META_LIMS_TABLE_2 = 1+3 ; // Table size
	public static int META_LIMS_TABLE_3 = 1+3 ; // Table size, BE SURE TO EDIT THESE VALES IF META TABLES HAVE COLUMN COUNT CHANGES
	public static int META_LIMS_TABLE_4 = 1+5 ; // Table size

	public static int tableSize( String table )
	{
		if ( table == "META_LIMS_TABLE_1" )
			return META_LIMS_TABLE_1 ;
		if ( table == "META_LIMS_TABLE_2" )
			return META_LIMS_TABLE_2 ;
		if ( table == "META_LIMS_TABLE_3" )
			return META_LIMS_TABLE_3 ;
		if ( table == "META_LIMS_TABLE_4" )
			return META_LIMS_TABLE_4 ;

		Vector<String> cols = getColumns(table) ;
		return cols.size() ;
	}

	public static Connection getConn()
	{
		try {
			if (util == null) {
				util = new JDBCUtilities() ; // Connects to server
			}
			return util.getConnection() ;
		} catch (FileNotFoundException e) {
			System.err.println( "FILE NOT FOUND" ) ;
			e.printStackTrace();
		} catch (InvalidPropertiesFormatException e) {
			System.err.println( "INVALID PROPERTIES FORMAT" ) ;
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println( "IO EXCEPTION" ) ;
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			e.printStackTrace();
		}
		return null ;
	}

	// Execute the given SQL command -- NOT safe against injection, use only for internal purposes
	public static boolean execute( String s )
	{
		Connection con = null;
		Statement statement = null;
		try {
			con = getConn();
			statement = con.createStatement() ;
			System.out.println( "SQL: " + s ) ;
			boolean b = statement.execute(s) ;
			return( b ) ;
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( s ) ;
			e.printStackTrace();
		} finally {
			closeStmt(statement);
		}
		return ( false ) ;
	}
	
	// Pop the last 'select' query off the stack and execute it
	public static Vector<Vector<String>> executeLastQuery(String table) {
		PreparedStatement prepStmt = util.popQuery(); // Execute the one before it
		if (prepStmt == null) {
			return null;
		} else {
			return selectPreparedStmt(table, prepStmt);
		}
	}
	
	
	// Execute the given select PreparedStatement and return query results
	public static Vector<Vector<String>> selectPreparedStmt(String table, PreparedStatement prepStmt) {
		ResultSet rs = null;
		int tableSize = tableSize(table);
		Vector<Vector<String>> out = new Vector<Vector<String>>();

		try {
			//Connection con = getConn();
			System.out.println( "SQL: " + prepStmt) ;
			rs = prepStmt.executeQuery();

			while( rs.next() )
			{
				String r = rs.getString(1) ; // RS is indexed from 1
				Vector<String> q = new Vector<String>() ;
				for( int i = 2 ; i <= tableSize ; i ++ )
				{
					q.add(r) ;
					r = rs.getString(i) ;
					if (rs.wasNull()) {
						r = "";
					}
				}
				q.add(r) ;
				out.add(q) ;
			}
			return out;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "" );
			e.printStackTrace();
		} finally {
			closeStmtAndResultSet(prepStmt, rs);
		}
		return out;
	}

	// Execute a batch of SQL inserts
	public static boolean insertBatch(String table, Vector<Vector<String>> values) {
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();
			con.setAutoCommit(false);
			
			int numCols = values.get(0).size();
			String insertStatement = "INSERT INTO " + table + " VALUES  (null";
			for (int i=0; i<numCols; i++) {
				insertStatement += ", ?";
			}
			insertStatement += ")";		
			prepStmt = con.prepareStatement(insertStatement);

			for (int i=0; i<values.size(); i++) {
				Vector<String> valueSet = values.get(i);
				
				// Set the parameters for the statement
				for (int j=0; j<numCols; j++) {
					prepStmt.setString(j+1, valueSet.get(j));
				}
				prepStmt.addBatch();

				if ((i + 1) % 1000 == 0) {
					prepStmt.executeBatch(); // Execute every 1000 items.
				}
			}
			// Execute the rest of the batch
			System.out.println("SQL: " + prepStmt);
			prepStmt.executeBatch();
			return( true ) ;
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION: Batch" ) ;
			e.printStackTrace();
		} finally {
			closeStmt(prepStmt);
		}
		return ( false ) ;
	}

	// Insert the given values into the table (if it is a valid table) and return the ID
	public static String insert( String table, Vector<String> values)
	{
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();

			// Construct INSERT statement
			int numCols = values.size();
			String insertStatement = "INSERT INTO " + table + " VALUES  (null";
			for (int i=0; i<numCols; i++) {
				insertStatement += ", ?";
			}
			insertStatement += ")";		
			prepStmt = con.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS);

			// Set the parameters for the statement
			for (int j=0; j<numCols; j++) {
				prepStmt.setString(j+1, values.get(j));
			}

			System.out.println( "SQL: " + prepStmt) ;
			prepStmt.executeUpdate();

			ResultSet rs = prepStmt.getGeneratedKeys();
			if (rs.next()) {
				Integer id = rs.getInt(1);
				return id.toString();
			} else {
				throw new SQLException();
			}
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Inserting values " + values + " into " + table);
			e.printStackTrace();
		} finally {
			closeStmt(prepStmt);
		}
		return null;
	}

	public static int setWhere(String table, String newCol, String newVal, Vector<String> columns, Vector<String> values) {
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();

			// Construct UPDATE statement
			int numCols = columns.size();
			String updateStatement = "UPDATE `" + table + "` SET `" + newCol + "`=?";
			if (numCols > 0) {
				updateStatement += " WHERE `" + columns.get(0) + "`=?";
				for (int i=1; i<numCols; i++) {
					updateStatement += " AND `" + columns.get(i) + "`=?";
				}
			}
			prepStmt = con.prepareStatement(updateStatement);

			// Set the parameters for the statement
			prepStmt.setString(1, newVal);

			for (int j=0; j<numCols; j++) {
				prepStmt.setString(j+2, values.get(j));
			}

			System.out.println( "SQL: " + prepStmt) ;
			int result = prepStmt.executeUpdate();
			return result;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Inserting values " + values + " into " + table);
			e.printStackTrace();
		} finally {
			closeStmt(prepStmt);
		}
		return -1;
	}

	// Same thing as other setWhere but when there is only one column/value pair to look for
	public static int setWhere(String table, String newCol, String newVal, String oldCol, String oldVal) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add(oldCol);
		vals.add(oldVal);
		return setWhere(table, newCol, newVal, cols, vals);
	}

	// Delete rows from the table
	public static int deleteWhere(String table, Vector<String> columns, Vector<String> values) {
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();

			// Construct DELETE statement
			String deleteStatement = "DELETE FROM `" + table + "` WHERE `" + columns.get(0) + "`=?";
			int numCols = columns.size();
			if (numCols > 1) {
				for (int i=1; i<numCols; i++) {
					deleteStatement += " AND `" + columns.get(i) + "`=?";
				}
			}

			prepStmt = con.prepareStatement(deleteStatement);

			// Set the parameters for the statement
			for (int j=0; j<numCols; j++) {
				prepStmt.setString(j+1, values.get(j));
			}

			System.out.println( "SQL: " + prepStmt) ;
			int result = prepStmt.executeUpdate();
			return result;
			
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Deleting rows with values " + values + "in columns" + columns + " from " + table);
			e.printStackTrace();
		} finally {
			closeStmt(prepStmt);
		}
		return -1;
	}

	// Delete from the table given only one column/value pair to look for
	public static int deleteWhere(String table, String column, String value) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add(column);
		vals.add(value);
		return deleteWhere(table, cols, vals);
	}

	// Select without supplying table size (e.g. for META table)
	public static Vector<Vector<String>> select( String table, Vector<String> columns, Vector<String> values, boolean push) {
		int tableSize = tableSize(table);
		return select(table, columns, values, tableSize, push);
	}

	// Select all from the given table
	public static Vector<Vector<String>> selectAll(String table, boolean push) {
		int tableSize = tableSize(table);
		return select(table, new Vector<String>(), new Vector<String>(), tableSize, push);
	}

	// Select from the given table (and if push is true, push the SQL statement onto the stack)
	public static Vector<Vector<String>> select( String table, Vector<String> columns, Vector<String> values, int tableSize, boolean push)
	{
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		Vector<Vector<String>> out = new Vector<Vector<String>>();

		try {
			con = getConn();

			// Construct SELECT statement
			int numCols = columns.size();
			String selectStatement = "SELECT * FROM " + table;
			if (numCols > 0) {
				selectStatement += " WHERE `" + columns.get(0) + "`=?";
				for (int i=1; i<numCols; i++) {
					selectStatement += " AND `" + columns.get(i) + "`=?";
				}
			}
			selectStatement += " ORDER BY id";
			prepStmt = con.prepareStatement(selectStatement);

			// Set the parameters for the statement
			for (int j=0; j<values.size(); j++) {
				prepStmt.setString(j+1, values.get(j));
			}

			System.out.println( "SQL: " + prepStmt) ;
			if (push) {
				util.pushQuery(prepStmt);
			}
			
			rs = prepStmt.executeQuery();

			while( rs.next() )
			{
				String r = rs.getString(1) ; // RS is indexed from 1
				Vector<String> q = new Vector<String>() ;
				for( int i = 2 ; i <= tableSize ; i ++ )
				{
					q.add(r) ;
					r = rs.getString(i) ;
					if (rs.wasNull()) {
						r = "";
					}
				}
				q.add(r) ;
				out.add(q) ;
			}
			return out;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Selecting values " + values + " from columns " + columns + " in "+ table);
			e.printStackTrace();
		} finally {
			// Keep the statement open if pushing it on the stack
			if (!push) {
				closeStmt(prepStmt);
			}
			closeResultSet(rs);
		}
		return out;
	}
	
	// Select from the given table where any of the given column/value pairs match
		public static Vector<Vector<String>> selectOR( String table, Vector<String> columns, Vector<String> values, int tableSize, boolean push)
		{
			Connection con = null;
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			Vector<Vector<String>> out = new Vector<Vector<String>>();

			try {
				con = getConn();

				// Construct SELECT statement
				int numCols = columns.size();
				String selectStatement = "SELECT * FROM " + table;
				if (numCols > 0) {
					selectStatement += " WHERE `" + columns.get(0) + "`=?";
					for (int i=1; i<numCols; i++) {
						selectStatement += " OR `" + columns.get(i) + "`=?";
					}
				}
				selectStatement += " ORDER BY id";
				prepStmt = con.prepareStatement(selectStatement);

				// Set the parameters for the statement
				for (int j=0; j<values.size(); j++) {
					prepStmt.setString(j+1, values.get(j));
				}

				System.out.println( "SQL: " + prepStmt) ;
				if (push) {
					util.pushQuery(prepStmt);
				}
				rs = prepStmt.executeQuery();

				while( rs.next() )
				{
					String r = rs.getString(1) ; // RS is indexed from 1
					if (rs.wasNull()) {
						r = "";
					}
					Vector<String> q = new Vector<String>() ;
					for( int i = 2 ; i <= tableSize ; i ++ )
					{
						q.add(r) ;
						r = rs.getString(i) ;
					}
					q.add(r) ;
					out.add(q) ;
				}

				return out;

			} catch (SQLException e) {
				System.err.println( "SQL EXCEPTION" ) ;
				System.err.println( "Selecting values " + values + " from columns " + columns + " in "+ table);
				e.printStackTrace();
			} finally {
				// Keep the statement open if pushing it on the stack
				if (!push) {
					closeStmt(prepStmt);
				}
				closeResultSet(rs);
			}
			return out;
		}

	// Select from the table any entries that contain the given values in the given columns
	public static Vector<Vector<String>> selectContains( String table, Vector<String> columns, Vector<String> values, int tableSize, boolean push)
	{
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		Vector<Vector<String>> out = new Vector<Vector<String>>();

		try {
			con = getConn();

			// Construct SELECT statement
			int numCols = columns.size();
			String selectStatement = "SELECT * FROM " + table;
			if (numCols > 0) {
				selectStatement += " WHERE `" + columns.get(0) + "` LIKE ?";
				for (int i=1; i<numCols; i++) {
					selectStatement += " OR `" + columns.get(i) + "` LIKE ?";
				}
			}
			selectStatement += " ORDER BY id";
			prepStmt = con.prepareStatement(selectStatement);

			// Set the parameters for the statement
			for (int j=0; j<values.size(); j++) {
				prepStmt.setString(j+1, "%" + values.get(j) + "%");
			}

			System.out.println( "SQL: " + prepStmt) ;
			if (push) {
				util.pushQuery(prepStmt);
			}
			rs = prepStmt.executeQuery();

			while( rs.next() )
			{
				String r = rs.getString(1) ; // RS is indexed from 1
				Vector<String> q = new Vector<String>() ;
				for( int i = 2 ; i <= tableSize ; i ++ )
				{
					q.add(r) ;
					r = rs.getString(i) ;
					if (rs.wasNull()) {
						r = "";
					}
				}
				q.add(r) ;
				out.add(q) ;
			}
			return out;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Selecting values " + values + " contained in columns " + columns + " in "+ table);
			e.printStackTrace();
		} finally {
			// Keep the statement open if pushing it on the stack
			if (!push) {
				closeStmt(prepStmt);
			}
			closeResultSet(rs);
		}
		return out;
	}

	// Select all items from the table by inserting the given values into the preformed statement
	public static Vector<Vector<String>> selectStatement(String table, String statement, Vector<String> values, boolean push) {
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		Vector<Vector<String>> out = new Vector<Vector<String>>();
		int tableSize = tableSize(table);

		try {
			con = getConn();

			statement += " ORDER BY id";
			prepStmt = con.prepareStatement(statement);

			// Set the parameters for the statement
			for (int j=0; j<values.size(); j++) {
				prepStmt.setString(j+1, values.get(j));
			}

			System.out.println( "SQL: " + prepStmt) ;
			rs = prepStmt.executeQuery();

			while( rs.next() )
			{
				String r = rs.getString(1) ; // RS is indexed from 1
				Vector<String> q = new Vector<String>() ;
				for( int i = 2 ; i <= tableSize ; i ++ )
				{
					q.add(r) ;
					r = rs.getString(i) ;
					if (rs.wasNull()) {
						r = "";
					}
				}
				q.add(r) ;
				out.add(q) ;
			}
			return out;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Selecting values " + values + " from "+ table + " using statement " + statement);
			e.printStackTrace();
		} finally {
			// Keep the statement open if pushing it on the stack
			if (!push) {
				closeStmt(prepStmt);
			}
			closeResultSet(rs);
		}
		return out;
	}

	// Find all rows containing term in any column
	public static Vector<Vector<String>> searchAllColumns(String table, String term, boolean push) {
		Vector<String> cols = getColumns(table);
		Vector<String> vals = new Vector<String>();
		for (int i=0; i<cols.size(); i++) {
			vals.add(term);
		}
		return selectContains(table, cols, vals, tableSize(table), push);
	}

	// Find all rows with the exact string term in any column
	public static Vector<Vector<String>> searchAllColumnsExact(String table, String term, boolean push) {
		Vector<String> cols = getColumns(table);
		Vector<String> vals = new Vector<String>();
		for (int i=0; i<cols.size(); i++) {
			vals.add(term);
		}
		return selectOR(table, cols, vals, tableSize(table), push);
	}

	// Find all rows with containing the string term in col
	public static Vector<Vector<String>> searchColumn ( String table , String val , String col , boolean push) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add(col);
		vals.add(val);
		return selectContains(table, cols, vals, tableSize(table), push);
	}

	// Find all rows with the exact string val in col
	public static Vector<Vector<String>> searchColumnExact ( String table, String val, String col , boolean push) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add(col);
		vals.add(val);
		return select(table, cols, vals, tableSize(table), push);
	}

	// Find all rows with the exact string val in col
	public static Vector<Vector<String>> searchColumnExact ( String table, String val, String col, int tableSize , boolean push) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add(col);
		vals.add(val);
		return select(table, cols, vals, tableSize, push);
	}
	
	// Change the name of a column
	public static boolean updateColumn( String table, String oldName, String newName) {
		newName = newName.trim();
		if (newName.length() > 0) {
			// Change name of column in table
			execute("ALTER TABLE `" + table + "` CHANGE `" + oldName + "` `" + newName + "` VARCHAR(100)");
			
			// Update templates
			Vector<String> oldCols = new Vector<String>();
			Vector<String> oldVals = new Vector<String>();
			oldCols.add("tables");
			oldCols.add("columns");
			oldVals.add(table);
			oldVals.add(oldName);
			setWhere("META_LIMS_TABLE_1", "columns", newName, oldCols, oldVals);
			
			// Update links
			oldCols.clear();
			oldCols.add("tableName");
			oldCols.add("tocolumn");
			setWhere("META_LIMS_TABLE_4", "tocolumn", newName, oldCols, oldVals);
			return true;
		} else {
			System.out.println("Invalid name");
			return false;
		}

	}
	
	// Add column to the table
	public static boolean addColumn( String table , String colname ) {
		colname = colname.trim();
		if (colname.length() > 0) {
			int length;
			if (colname.equals("download")) {
				length = 500;
			} else {
				length = 100;
			}
			execute( "ALTER TABLE `" + table + "` ADD `" + colname + "` VARCHAR(" + length + ");" ) ;
			return true;
		} else {
			System.out.println("Invalid name");
			return false;
		}
	}

	// Remove column from the table
	public static boolean removeColumn( String table , String colname )
	{
		return execute( "ALTER TABLE `" + table + "` DROP COLUMN `" + colname + "` ;" ) ;
	}

	// Get all columns from the given table
	public static Vector<String> getColumns( String table) {
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			con = getConn();
			Vector<String> out = new Vector<String>();

			// Construct the SELECT statement
			String selectStatement = "SELECT column_name FROM information_schema.columns WHERE table_name=?";	
			prepStmt = con.prepareStatement(selectStatement);

			// Set the parameter for the statement
			prepStmt.setString(1, table);

			System.out.println( "SQL: " + prepStmt) ;
			rs = prepStmt.executeQuery();

			while( rs.next() )
			{
				String r = rs.getString(1) ; // RS is indexed from 1
				out.add(r) ;
			}
			return( out ) ;
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Getting columns from " + table) ;
			e.printStackTrace();
		} finally {
			closeStmtAndResultSet(prepStmt, rs);
		}
		return null ;
	}

	public static Set<String> getColumnSet( String table ) {
		Vector<String> colVec = getColumns(table);
		return SQLUtil.toSet(colVec);
	}

	// Finds the index of the given column in the table (returns -1 if the column doesn't exist)
	public static int getColumnIndex(String table, String colName) {
		int idx = -1;
		Vector<String> orderedCols = getColumns(table);
		for (int i = 0; i<orderedCols.size(); i++) {
			if (orderedCols.get(i).equals(colName)) {
				idx = i;
			}
		}
		return idx;
	}

	public static Set<String> colSet( String table , int col )
	{
		Vector<Vector<String>> temp = SQL2.selectAll( table , false);
		if ( temp == null )
			return null ;
		Vector<String> vec = SQLUtil.getCol( col , temp ) ;
		if( vec == null )
			return ( null ) ;
		return SQLUtil.toSet( vec ) ;
	}

	// Get the names of all the templates in the table
	public static Set<String> getTemplates( String table )
	{
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("tables");
		vals.add(table);

		Vector<Vector<String>> temp = select("META_LIMS_TABLE_1", cols, vals, false);
		if ( temp == null ) {
			return null ;
		}
		Set<String> out = new HashSet<String>() ;
		for( Vector<String> vs : temp )
		{
			if ( vs.get(2) != null && vs.get(2).length() > 0 )	// table headers in META_LIMS_TABLE_1 hold a null entry under 'templates'
				out.add( vs.get(2) ) ;
		}
		return out ;
	}

	// Determine if the given template exists
	public static boolean isTemplate( String table , String template )
	{
		if ( template == null ) {
			return false ;
		}
		Set<String> templates = getTemplates( table ) ;
		return templates.contains(template);
	}

	// Get the set of columns belonging to the template
	public static Set<String> getTemplateColumns( String table , String template )
	{
		Vector<String> templateVec = getTemplateColumnVec(table, template);
		if (templateVec == null) {
			return null;
		} else {
			return SQLUtil.toSet(templateVec);
		}
	}

	// Get the set of columns belonging to the template, as a vector
	public static Vector<String> getTemplateColumnVec( String table , String template )
	{
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("tables");
		cols.add("template");
		vals.add(table);
		vals.add(template);

		Vector<Vector<String>> temp = select("META_LIMS_TABLE_1", cols, vals, false) ;
		if ( temp == null )
			return null ;
		Vector<String> out = new Vector<String>() ;
		for( Vector<String> vs : temp )
		{
			if ( vs.get(3) != null )	// templates headers in META_LIMS_TABLE_1 hold a null entry under 'columns'
				out.add( vs.get(3) ) ;
		}
		return out ;
	}

	public static int createTable ( String name ) {
		name = name.trim();
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();

			// Construct CREATE TABLE statement
			String selectStatement =  "CREATE TABLE `" + name + "` (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id)) ;";
			prepStmt = con.prepareStatement(selectStatement);

			System.out.println( "SQL: " + prepStmt) ;
			int result = prepStmt.executeUpdate();
			return( result) ;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Creating table " + name);
			e.printStackTrace();
			
		} finally {
			closeStmt(prepStmt);
		}
		return -1;
	}
	
	// Creates newTable, and copies all columns and contents of oldTable into newTable
	public static void copyTable (String oldTable, String newTable) {
		copyMetaTable1(oldTable, newTable);
		createTable(newTable);
		
		Vector<String> columns = getColumns(oldTable);
		columns.remove("id");
		for (String col : columns) {
			addColumn(newTable, col);
		}
		execute("INSERT INTO `" + newTable + "` SELECT * FROM `" + oldTable + "` ;") ;
		copyMetaTable3(oldTable, newTable);
		copyMetaTable4(oldTable, newTable);
	}
	
	// Copy all the information in META_LIMS_TABLE_1 about oldTable to newTable
	private static void copyMetaTable1(String oldTable, String newTable) {
		execute(" INSERT INTO META_LIMS_TABLE_1 (META_LIMS_TABLE_1.id, META_LIMS_TABLE_1.tables, META_LIMS_TABLE_1.template, META_LIMS_TABLE_1.columns) " + 
				"(SELECT null, '" + newTable + "', META_LIMS_TABLE_1.template, META_LIMS_TABLE_1.columns " + 
				"FROM META_LIMS_TABLE_1 " + 
				"WHERE META_LIMS_TABLE_1.tables = '" + oldTable + "');");
	}
	
	// Copy the root id and path prefix from oldTable to newTable
	private static void copyMetaTable3 (String oldTable, String newTable) {
		execute(" INSERT INTO META_LIMS_TABLE_3 (META_LIMS_TABLE_3.id, META_LIMS_TABLE_3.setting, META_LIMS_TABLE_3.setKey, META_LIMS_TABLE_3.setValue) " + 
				"(SELECT null, META_LIMS_TABLE_3.setting, '" + newTable + "', META_LIMS_TABLE_3.setValue " + 
				"FROM META_LIMS_TABLE_3 " + 
				"WHERE META_LIMS_TABLE_3.setKey = '" + oldTable + "' AND META_LIMS_TABLE_3.setting = 'pathPrefix');");
		
		execute(" INSERT INTO META_LIMS_TABLE_3 (META_LIMS_TABLE_3.id, META_LIMS_TABLE_3.setting, META_LIMS_TABLE_3.setKey, META_LIMS_TABLE_3.setValue) " + 
				"(SELECT null, META_LIMS_TABLE_3.setting, '" + newTable + "', META_LIMS_TABLE_3.setValue " + 
				"FROM META_LIMS_TABLE_3 " + 
				"WHERE META_LIMS_TABLE_3.setKey = '" + oldTable + "' AND META_LIMS_TABLE_3.setting = 'rootID');");
	}
	
	// Copy all the links from the old table to the new table
	private static void copyMetaTable4 (String oldTable, String newTable) {
		execute(" INSERT INTO META_LIMS_TABLE_4 (META_LIMS_TABLE_4.id, META_LIMS_TABLE_4.tableName, META_LIMS_TABLE_4.linkfrom, META_LIMS_TABLE_4.linkto, META_LIMS_TABLE_4.tocolumn, META_LIMS_TABLE_4.linktype) " + 
				"(SELECT null, '" + newTable + "', META_LIMS_TABLE_4.linkfrom, META_LIMS_TABLE_4.linkto, META_LIMS_TABLE_4.tocolumn, META_LIMS_TABLE_4.linktype " + 
				"FROM META_LIMS_TABLE_4 " + 
				"WHERE META_LIMS_TABLE_4.tableName = '" + oldTable + "');");
	}

	public static int removeTable ( String name )
	{
		Connection con = null;
		PreparedStatement prepStmt = null;
		try {
			con = getConn();

			// Construct DROP TABLE statement
			String selectStatement =  "DROP TABLE `" + name + "` ;";
			prepStmt = con.prepareStatement(selectStatement);

			System.out.println( "SQL: " + prepStmt) ;
			int result = prepStmt.executeUpdate();
			return( result) ;

		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Removing table " + name);
			e.printStackTrace();
		} finally {
			closeStmt(prepStmt);
		}
		return -1;
	}

	public static Set<String> getTables() {
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		Set<String> tables = new HashSet<String>();

		try {
			con = getConn();

			// Construct the SELECT statement
			String selectStatement = "SELECT table_name FROM information_schema.tables WHERE table_schema='lims'";	
			prepStmt = con.prepareStatement(selectStatement);

			System.out.println( "SQL: " + prepStmt) ;
			rs = prepStmt.executeQuery();

			while ( rs.next()) {
				String table = rs.getString(1);
				if (!isReserved(table)) {
					tables.add(table);
				}
			}
			return tables;
		} catch (SQLException e) {
			System.err.println( "SQL EXCEPTION" ) ;
			System.err.println( "Getting all tables") ;
			e.printStackTrace();
		} finally {
			closeStmtAndResultSet(prepStmt, rs);
		}
		return tables ;
	}

	public static boolean isReserved( String s )
	{
		if ( SQL2.stringCompare( s , "id" ) )
			return true ;
		if ( SQL2.stringCompare( s , "template" ) )
			return true ;
		if ( SQL2.stringCompare( s , "download" ))
			return true ;
		if ( SQL2.stringCompare( s , "isDirectory" ))
			return true ;
		if ( SQL2.stringCompare( s , "name" ))
			return true ;
		if ( SQL2.stringCompare( s , "index" ))
			return true ;
		if ( SQL2.stringCompare( s , "DefaultTemplate" ))
			return true ;
		if ( SQL2.stringCompare( s , "META_LIMS_TABLE_1" ) )
			return true ;
		if ( SQL2.stringCompare( s , "META_LIMS_TABLE_2" ) )
			return true ;
		if ( SQL2.stringCompare( s , "META_LIMS_TABLE_3" ) )
			return true ;
		if ( SQL2.stringCompare( s , "META_LIMS_TABLE_4" ) )
			return true ;
		return false ;
	}

	// Determine whether a column is editable by a user adding an item
	public static boolean isEditable( String column ) {
		if ( SQL2.stringCompare( column , "id" ) )
			return false ;
		if ( SQL2.stringCompare( column , "template" ) )
			return false ;
		return true ;
	}

	public static boolean stringCompare( String sqlString , String javaString )
	{
		if (sqlString == null && javaString == null) {
			return true;
		}
		if (sqlString == null || javaString == null) {
			return false;
		}
		if ( sqlString.length() != javaString.length() )
			return false ;
		char[] sql = sqlString.toCharArray() ;
		char[] java = javaString.toCharArray() ;
		for( int i = 0 ; i < javaString.length() ; i ++ )
		{
			if ( sql[i] != java[i] )
				return false ;
		}
		return true ;
	}
	
	// Close the given statement and result set
	private static void closeStmtAndResultSet(Statement stmt, ResultSet rs) {
		try {
			if (rs != null) rs.close();
			} catch (Exception e) {};
	    try {
	    	if (stmt != null) stmt.close();
	    	} catch (Exception e) {};
	}
	
	// Close the given statement and result set
	private static void closeStmt(Statement stmt) {
	    try {
	    	if (stmt != null) stmt.close();
	    	} catch (Exception e) {};
	}
	
	private static void closeResultSet(ResultSet rs) {
		try {
			if (rs != null) rs.close();
		} catch (Exception e) {};
	}
	
	// When program closes, close connection
	public static void closeConnection() {
		try {
			Connection conn = getConn();
			if (conn != null) {
				conn.close();
				System.out.println("Connection closed");
			}
		} catch (Exception e) {};
	}
}
