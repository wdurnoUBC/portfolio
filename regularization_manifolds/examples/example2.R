
# We compare many factor analysis models to a full covariance model when the actual model is a factor analysis model. 

# parameters 
p = 50 
M = 3  

# libraries 

# generate factor analysis parameters ( L0 , Psi0 )  
sigma_tmp = cov2cor( rWishart( 1 , p*p + p , diag(p)+0.25 )[,,1] ) 
e = eigen( sigma_tmp ) 
L0 = as.matrix(e$vectors[,1:M]) 
for( k in 1:M ) 
{ 
	L0[,k] = L0[,k] * sqrt( e$values[k] ) 
} 
Psi0 = sqrt( diag( sigma_tmp - L0 %*% t(L0) ) ) 

Sigma0 = cov2cor( L0 %*% t(L0) + diag( Psi0^2 ) ) 

# sample generation function 
generate_samples = function( n = 100 ) 
{ 
	generate_sample = function(i) L0 %*% rnorm(M) + Psi0 * rnorm(p) 
	ll = lapply( 1:n , generate_sample ) 
	matrix( unlist(ll) , nrow = n ) 
} 

# estimation functions 
estimate_big_model = function( x ) cor( x ) 

estimate_small_model = function( n , m=1 ) 
{ 
	fit = NULL 
	fail = TRUE 
	while( fail ) 
	{ 
		try({ 
		fit = factanal( generate_samples(n) , factors=m, rotation="varimax") 
		fail = FALSE 
		}) 
	} 
	L1 = fit$loadings[ 1:p , 1:m ] 
	Psi1_sq = diag( 1 - L1 %*% t(L1) ) 
	L1 %*% t(L1) + diag(Psi1_sq) # error is measured in implicit space, not actual. This deviates from proven theory! 
}  

# Error per model 
big_model_error = function( n ) sum( as.vector( Sigma0 - estimate_big_model( generate_samples(n) ) )^2 ) 
small_model_error = function( n , m=1 ) sum( as.vector( Sigma0 - estimate_small_model( n , m ) )^2 ) 

# Average error per model 

avg_err_big = function( n , n2=100 ) mean( unlist( lapply( 1:n2 , function(i) big_model_error(n) ) ) ) 
avg_err_small = function( n , n2=100 , m=1 ) mean( unlist( lapply( 1:n2 , function(i) small_model_error(n, m) ) ) ) 

# DEMONSTRATION 

avg_err_big( 100 , 100 ) 
avg_err_small( 100 , 100 ) 

illustrate_helper = function( n = 51 , bound=Inf )  
{ 
	m_vec = 2:(min(n,p,bound)-1) 
	e_vec = 2:(min(n,p,bound)-1) 
	for( i in 1:length(m_vec) ) 
	{ 
		e_vec[i] = avg_err_small( n , 100 , m_vec[i] ) 
		print( paste0( i , " of " , length(m_vec) ) ) 
	} 
	
	m_vec = c( m_vec , min(n,p,bound) )  
	e_vec = c( e_vec , avg_err_big( n , 100 ) ) 
	
	out = cbind( m_vec , e_vec ) 
	colnames( out ) = c( "m" , "e" ) 
	out  
} 

n100 = illustrate_helper( 100 , bound=40 ) 
plot( e ~ m , n100 )  





