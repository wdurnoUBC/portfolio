fmwc_est_c.so: fmwc_est_c.c fmwc_est_cu.so  
	R CMD SHLIB fmwc_est_c.c fmwc_est_cu.so 

fmwc_est_cu.so: fmwc_est_cu.cu
	nvcc --compiler-options '-fPIC' -o fmwc_est_cu.so --shared fmwc_est_cu.cu

clean:
	rm *.o
	rm *.so
