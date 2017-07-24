// Evan's Fasta subsetter 
// I needed a C++ implementation since Python was too slow & inefficient.  
#include <iostream> 
#include <fstream> 
#include <unordered_set> 
// #include <underdered_map>
using namespace std ; 

int main ( int argc, char *argv[] ) 
{ 
	int err = 0 ; 
	
	// Check for Arg1 
	if( argc != 2 ) 
		err = 1 ; 
	
	if( err ) 
	{ 
		cerr << "Fasta subsetter:" << endl ; 
		cerr << "Subsets fasta files by headers" << endl ; 
		cerr << "Implementation: C++ HashMap" << endl ; 
		cerr << "Usage:" << endl ; 
		cerr << "Arg1: header flat file, headers separated by new line characters, '>' characters omitted from headers" << endl ; 
		cerr << "stdin: fasta file" << endl ; 
		cerr << "stdout : subsetted fasta" << endl ; 
	} 
	
	// Initiate HashSet  
	unordered_set < string > headers ; 
	
	// Load headers into HashMap 
        ifstream infile( argv[1] );
        string line ;
        while( getline(infile, line) )
        { 
		headers.insert( line ) ; 
        }  
	
	// stream the Fasta file, checking for headers 
	int header_contained = 0 ; 
	unordered_set <string> :: const_iterator got ; 
	while( getline( cin , line ) )  
	{ 
		// Check for header indicator '>' 
		if( line.size() > 0 ) 
		{ 
			if( line[0] == '>' ) 
			{ 
				line = line.substr( 1 ) ;  
				got = headers.find( line ) ; 
				if( got == headers.end() ) 
				{ 
					// Not found 
					header_contained = 0 ; 
				} 
				else 
				{ 
					// Found 
					header_contained = 1 ; 
					cout << ">" ; 
				} 
			} 
		} 
		
		// Stream contents to standard out if the header was contained  
		if( header_contained )  
			cout << line << endl ; 
	} 
	
	return( 0 ) ; 
} 

