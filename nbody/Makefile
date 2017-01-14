
nbodyMPI: nbodyMPI.c
	mpicc nbodyMPI.c -o nbodyMPI -lm

nbodyCUDA: nbodyCUDA.cu
	nvcc nbodyCUDA.cu -o nbodyCUDA -arch sm_20

clean:
	rm nbodyMPI 
	rm nbodyCUDA 

