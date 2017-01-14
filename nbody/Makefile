
nbodyMPI nbodyCUDA: nbodyMPI.c nbodyCUDA.cu
	mpicc nbodyMPI.c -o nbodyMPI -lm 
	nvcc nbodyCUDA.cu -o nbodyCUDA -arch sm_20 

clean:
	rm nbodyMPI 
	rm nbodyCUDA 

