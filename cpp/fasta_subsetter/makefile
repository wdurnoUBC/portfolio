fasta_subsetter.exe: fasta_subsetter.cpp
	g++ -std=c++11 fasta_subsetter.cpp -o fasta_subsetter.exe 

test:
	cat test_files/test.fasta | ./fasta_subsetter.exe test_files/test.headers > test_files/test_out

clean: 
	rm fasta_subsetter.exe 
	rm test_files/test_out
