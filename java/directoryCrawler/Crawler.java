package directoryCrawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Crawler
{

        private File root = null ;
        private int rootPathLength;
        private Map<File,String> names = null ;
        private Map<File, Set<File> > tree = null ;
        private Map<File, Boolean> isDir = null ;

        public Crawler( File root )
        {
                this.root = root ;
                this.rootPathLength = root.getAbsolutePath().length();
                names = new HashMap<File,String>() ;
                names.put(root, fullPath(root) ) ;
                tree = new HashMap<File,Set<File>>() ;
                tree.put(root, new HashSet<File>() ) ;
                isDir = new HashMap<File,Boolean>() ;
                isDir.put(root, root.isDirectory() ) ;
        }

        public String getName( File f )
        {
                return names.get(f) ;
        }

        public Set<File> getSub( File f )
        {
                return tree.get(f) ;
        }

        public File getRoot()
        {
                return root ;
        }

        public Set<File> getFiles()
        {
                return names.keySet() ;
        }

        public Boolean isDirectory( File f )
        {
                return isDir.get( f ) ;
        }
        
        private String fullPath( File f )
        {
        	if (f.getAbsolutePath().length() == rootPathLength) {
        		// Root directory
        		return File.separator;
        	}
        	return f.getAbsolutePath().substring(rootPathLength);
        }

//        private String name( File f )
//        {
//        	String[] list = f.getAbsolutePath().split( "/" ) ;                              // UNIX SPECIFIC --- use File.separator?
//        	if( list.length == 0 )
//        		return "/" ;
//        	else
//        	{
//        		int l = list.length ;
//        		//                        if ( f.isDirectory() )
//        		//                                return(( list[l-1] + "/" )) ; // Directory names followed by slash
//        		//                        else
//        		return list[l-1] ;
//        	}
//        }

        public void initialCrawl() throws FileNotFoundException
        {
                crawlHere( root ) ;
        }

        private void crawlHere( File f ) throws FileNotFoundException
        {
                File[] sub1 = f.listFiles() ;
                if (sub1 == null) {
                	throw new FileNotFoundException();
                }
                for( File g : sub1 )
                {
                	String name = g.getName();
                	if (!name.matches("\\..*")) {     // Exclude hidden files
                		put(g,f);
                		if (g.isDirectory()) {
                			crawlHere(g);
                		}
                	}
                }
        }
        
      private void put( File f , File parent )
      {
    	  names.put( f , fullPath(f) ) ;
    	  isDir.put( f , f.isDirectory() ) ;
    	  tree.get( parent ).add( f ) ;
    	  if ( !tree.containsKey(f) )
    		  tree.put( f , new HashSet<File>() ) ;
      }

}