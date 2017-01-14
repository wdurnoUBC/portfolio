
source("fmwc_regression.R") 

# User interface to FMWC (Floor Model With Copula) multivariate regression tool 
# The FMWC model is for regressing high-dimensional count data y on regressor matrix x. 
# This software is GPU-accelerated and requires CUDA.  
# Exact model definition is available from my thesis (not yet published!)  
#    
# Features to be implemented: 
# 1. L-BFGS optimization  
#  

# Multivariate count regression estimation 
# Count distributions are dependent through Gaussian copula with a factor model. 
# The factor model has m factors.   
fmwc_regression = function(y,x,formula=~.,m=3,n_bootstraps=1,CPU_threads=16,...) 
{ 
	if( n_bootstraps == 1 ) 
	{ 
		return( gpu_estimate(y,x,formula,m,...) ) 
	}else{ 
		return( get_many_bootstraps(y,x,n_threads=CPU_threads,n_bootstraps,formula,m,gpu=T,...) )  
	} 
} 


