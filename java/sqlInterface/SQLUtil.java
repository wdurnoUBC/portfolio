package sqlInterface;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLUtil 
{
	public static Vector<String> getRow( int i , Vector<Vector<String>> vvs )
	{
		if ( i < 0 )
			return null ;
		if( i >= vvs.size() )
			return null ;
		return vvs.get(i) ;
	}
	
	public static Vector<String> getCol( int i , Vector<Vector<String>> vvs )
	{
		if ( i < 0 )
			return null ;
		Vector<String> out = new Vector<String>();
		for( int j = 0 ; j < vvs.size() ; j ++ )
		{
			if ( i >= vvs.get(j).size() )
				return null ;
			String s = vvs.get(j).get(i);
			if (!out.contains(s)) {
				out.add( s ) ;
			}
		}
		return out;
	}
	
	public static String intToString( int x )
	{
		return ("" + x) ;
	}
	
	public static int stringToInt ( String s )
	{
		int aInt = Integer.parseInt(s);
		return aInt ;
	}
	
	public static Set<String> toSet ( Vector<String> vs )
	{
		Set<String> out = new HashSet<String>() ;
		for( int i = 0 ; i < vs.size() ; i ++ )
		{
			out.add( vs.get(i) ) ;
		}
		return out ;
	}
	
	public static boolean setContainsSQLString ( Set<String> ss , String element )
	{
		for( String s : ss )
		{
			if ( SQL2.stringCompare(element , s ) )
			{
				return true ;
			}
		}
		return false ;
	}
	
	public static boolean vectorContainsSQLString ( Vector<String> ss , String element )
	{
		for( String s : ss )
		{
			if ( SQL2.stringCompare(element , s ) )
			{
				return true ;
			}
		}
		return false ;
	}
	
	// Determine whether the string can be converted to an integer
	public static boolean isAnInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e){
			return false;
		}
	}
	
	public static boolean isValidName(String name) {
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);

		return !m.find();
	}
	
}
