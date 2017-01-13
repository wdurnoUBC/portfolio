
# User interface to FMWC multivariate regression 
# The FMWC model is for regressing high-dimensional count data y on regressor matrix x. 
# This software is GPU-accelerated and requires CUDA.  
# Exact model definition is available from ... thesis? 
# through bootstrapping is available. 
#  
# Features to be interfaced: 
# 1. Bootstrap significance testing 
# 2. Approximate Hessian significance testing via Delta rule  
# 3. Correlation network inference with and without mixed effects 
#    
# Features to be implemented: 
# 1. L-BFGS optimization  
#  
fmwc_regression = function(y,x,formula=~.) 
{ 
	
} 

