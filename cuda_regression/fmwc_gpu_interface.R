
dyn.load("fmwc_est_c.so") 

# void fmwc_gpu_mle ( double *y , double *x , double *t , int *n , int *p , int *q , int *tn , int *m , double *eps , int *batch_size , int *max_it , double *test_coefs , int *test_coefs_n , int *min_it , double *out ) 
fmwc_gpu_mle = function( y , x , t , formula = ~ . , m=5 , rel_eps=0.001 , batch_size=NA , min_it=5 , max_it=1000 , test_coefs=(-20:200 + 0.5)/200 , n_iter=10000 , seed=NULL ) 
{ 
	n = nrow(y) 
	no_na_idx = (1:n)[ rowSums( is.na(y) ) + rowSums( is.na(x) ) + rowSums( is.infinite(as.matrix(y)) ) + rowSums( is.infinite(as.matrix(x)) ) == 0 ]
        no_na_y = y[ no_na_idx ,] 
	cnx = colnames(x) 
        no_na_x = as.data.frame( x[ no_na_idx ,] ) 
	colnames( no_na_x ) = cnx 
	yy = as.matrix(no_na_y) 
	xx = model.matrix( formula , no_na_x ) 
	th = t  
	tn = length(th) 
	n = nrow(yy) 
	if( nrow(xx) != n ){ print("y and x must have the same number of rows!") ; return(NA) } 
	p = ncol(yy)  
	q = ncol(xx) 
	if( is.na(batch_size) ) { batch_size = max(1, floor(1023 * 65536 / ( max(p,n) * p )) ) } # auto-generate batch sizes 
	if( is.null(seed) ){ seed = floor( 10000*runif(1) ) } 
	test_coefs_n = length( test_coefs ) 
	tmp = .C( "fmwc_gpu_mle" , as.double(yy) , as.double(xx) , as.double(th) , as.integer(n) , as.integer(p) , 
		as.integer(q) , as.integer(tn) , as.integer(m) , as.double(rel_eps) , as.integer(batch_size) ,
		 as.integer(max_it) , as.double(test_coefs) , as.integer(test_coefs_n) , as.integer(n_iter) , 
		as.integer(seed) , as.integer(min_it) , as.double(1) ) 
	out = list() 
	out$mle = tmp[[3]] 
	out$loglik = tmp[[17]] 
	out$iter_n = tmp[[11]] 
	out 
} 

# void log_lik_c ( double *y , double *x , double *t , int *n , int *p , int *q , int *tn , int *K , int *m , int *n_iter , int *seed , double *out ) 
# n_iter=10K gives about 3 decimals of precision on my 1980 parameter data set (m=5)  
fmwc_gpu_log_lik = function( y , x , t , formula = ~ . , m=5 , n_iter=10000 , seed=NULL ) 
{ 
        yy = as.matrix(y) 
        xx = model.matrix( formula , x ) 
        th = t  
        tn = length(th) 
        n = nrow(y) 
        if( nrow(xx) != n ){ printf("y and x must have the same number of rows!") ; return(NA) } 
        p = ncol(yy) 
        q = ncol(xx) 
        if( is.null(seed) ){ seed = floor( 10000*runif(1) ) } 
	.C( "log_lik_c" , as.double(yy) , as.double(xx) , as.double(th) , as.integer(n) , as.integer(p) , 
		as.integer(q) , as.integer(tn) , as.integer(1) , as.integer(m) , as.integer(n_iter) , as.integer(seed) , as.double(1) )[[12]] 
} 

fmwc_bootstrap = function( y , x , t , n_bootstraps=100 , ... ) 
{ 
	f = function(i) 
	{ 
		n = nrow(y) 
		idx = sample( 1:n , n , replace=T ) 
		yy = y[idx,] 
		xx = x[idx,] 
		fmwc_gpu_mle( yy , xx , t , ... ) 
	} 
	lapply( 1:n_bootstraps , f ) 
} 



