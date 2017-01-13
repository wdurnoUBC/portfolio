
library(parallel)  

# Load GPU interface  
source("fmwc_gpu_interface.R") 

# returns an initialized estimate ready for Newton-Raphson optimization 
# t is of the form [ beta_q , lnu , lsig , L_m , lPsi ]_p 
lt_prototype_est = function(y,x,formula=~ .,m=5,nu0=1.5,robust=T,robust.sig=1e-5) 
{ 
	if( ! is.matrix(y) & ! is.data.frame(y) & ! is.matrix(x) & ! is.data.frame(x) ){ return(-1) } 
	n = nrow(y) 
	p = ncol(y) 
	q = ncol(x) 
	if( nrow(x) != n ){ return(-2) } 
	# Remove NAs 
	no_na_idx = (1:n)[ rowSums( is.na(y) ) + rowSums( is.na(x) ) + rowSums( is.infinite(as.matrix(y)) ) + rowSums( is.infinite(as.matrix(x)) ) == 0 ] 
	no_na_y = y[ no_na_idx ,] 
	cnx = colnames(x) 
	no_na_x = as.data.frame(x[ no_na_idx ,]) 
	colnames(no_na_x) = cnx 
	xx = model.matrix( formula , no_na_x ) 
	q = ncol(xx) 
	ly = log( as.matrix(no_na_y) + 1 ) 
	n = nrow( ly ) 
	# OLS univariate estimates 
	mdl = lm.fit( xx , ly ) 
	# Median-based sig estimates  
	nu0_const = (nu0-1) * beta( nu0*0.5 , 0.5 ) / sqrt(nu0) 
	mean_deviations = function(j) mean( abs(mdl$residuals[,j]) ) 
	mean_deviations = sapply( 1:p , mean_deviations ) 
	sig0 = mean_deviations * nu0_const
	# nonlinear optimize fit for nu 
	neg_t_log_lik1 = function(j,nu) -sum( dt( mdl$residuals[,j]/sig0[j] , nu , log=T ) ) + log(sig0[j]) 
	nu1 = function(j) 
	{ 
		try({ return( optimize( function(nu) neg_t_log_lik1(j,nu) , c(1.1,10000) )$minimum ) }, silent = robust ) # only print errors if they're not caught 
		if(robust){ return(nu0) }else{ return(NA) } 
	} 
	nu1 = sapply( 1:p , nu1 ) 
	# nonlinaer optimize fit for (sig,nu) 
	neg_t_log_lik2 = function(j,lnu,lsig) -sum( dt( mdl$residuals[,j]/exp(lsig) , exp(lnu) , log=T ) ) + lsig  
	par2 = function(j) 
	{ 
		try({return( exp( optim( function(par) neg_t_log_lik2( j , par[1] , par[2] ) , par=c( log(nu1[j]) , log(sig0[j]) ) )$par ) )}, silent = robust ) 
		if(robust)
		{ 
			if( sig0[j] > 0 ){return( c( nu1[j] , sig0[j] ) )}else{return( c( nu1[j] , robust.sig ) )} 
		}else{ return( c(NA,NA) ) } 
	} 
	par2 = lapply( 1:p , par2 )  
	par2 = t( matrix( unlist(par2) , nrow=2 ) ) 
	colnames( par2 ) = c("nu2","sig2") 
	# nonlinear optimize fit for all  
	neg_t_log_lik3 = function(j,beta,lnu,lsig) -sum( dt( ly[,j]/exp(lsig) - xx %*% beta , exp(lnu) , log=T ) ) + lsig 
	silent = F 
	par3 = function(j) 
	{ 
		beta = mdl$coefficients[,j] 
		lnu = log(par2[j,1]) 
		lsig = log(par2[j,2]) 
		try({ 
		par = optim( function(par) neg_t_log_lik3(j,par[1:q],par[q+1],par[q+2]) , par=c( beta , lnu , lsig ) , method="BFGS" )$par 
		par[ (q+1):(q+2) ] = exp( par[ (q+1):(q+2) ] ) 
		return(par) 
		},silent = robust) 
		if(robust){ return( c(beta,par2[j,1:2]) ) }else{ return( rep(NA,q+2) ) } 
	} 
	par3 = lapply( 1:p , par3 ) 
	par3 = t(matrix( unlist(par3) , nrow=q+2 )) 
	colnames(par3) = c( colnames(xx) , "nu" , "sig" ) 
	# nonlinear optimize for actual floor model, Y = [ exp( sig * ( T - xx * beta ) ) ] 
	neg_t_log_lik4 = function(j,beta,lnu,lsig) 
	{ 
		hi = log(no_na_y[,j]+1)/exp(lsig) - xx %*% beta 
		lo = log(no_na_y[,j])/exp(lsig) - xx %*% beta 
		-sum( log(pt( hi , exp(lnu) ) - pt( lo , exp(lnu) )) ) 
	} 
        par4 = function(j)  
        { 
                beta = par3[j,1:q]
                lnu = log(par3[j,q+1]) 
                lsig = log(par3[j,q+2]) 
		try({ 
                par = optim( function(par) neg_t_log_lik4(j,par[1:q],par[q+1],par[q+2]) , par=c( beta , lnu , lsig ) , method="BFGS" )$par 
                par[ (q+1):(q+2) ] = exp( par[ (q+1):(q+2) ] ) 
                return(par) 
		},silent = robust) 
		if(robust){return( par3[j,] )}else{return( rep(NA,q+2) )} 
        }
	par4 = lapply( 1:p , par4 ) 
	par4 = t(matrix( unlist(par4) , nrow=q+2 )) 	
	colnames(par4) = c( colnames(xx) , "nu" , "sig" ) 
	# Calc residual cov 
	qFET = function(y,par) # quantile function of the floor exponential t 
	{ 
		beta = par[1:q] 
		nu = par[q+1] 
		sig = par[q+2] 
		return(log(y+1)/sig - xx%*%beta) # DEBUG 
		qnorm( pt( log(y+1)/sig - xx%*%beta , nu ) ) 
	}  
	qMat = lapply( 1:p , function(j) qFET( no_na_y[,j] , par4[j,] ) ) 
	qMat = matrix( unlist(qMat) , nrow=n ) 
	# Eigen-decomposition   
	ei = eigen( cor( qMat ) , symmetric=T ) 
	L = t( t( ei$vectors[,1:m] ) * sqrt(ei$values[1:m]) ) 
	correct_sign = function(k) 
	{ 
		if( sum( sign(L[,k]) ) > 0 ){ return(L[,k]) }  
		-L[,k] # mostly negative, swap signs 
	} 
	L = matrix( unlist(lapply( 1:m , correct_sign )) , ncol=m ) 
	psi = 1 - rowSums( L * L ) 
	out = cbind( par4 , L , psi ) 
	colnames( out )[ (q+2+1):(q+2+m+1) ] = c( paste0( "factor" , 1:m ) , "psi" ) 
	out 
} 

# converts a matrix R parameters to a matrix of GPU parameters  
# each column is a new parameter  
t_to_gpu = function(t,q=4,p=360,m=5) 
{ 
	t = as.matrix(t) 
	# extract entire t columns in gpu-format  
	get_column = function(k) 
	{ 
		get_beta = function(j) t[ ((1:q)-1)*p + j ,k]  
		get_lsig = function(j) log( t[ (q+2-1)*p + j ,k] ) 
		get_lnu  = function(j) log( t[ (q+1-1)*p + j ,k] ) 
		get_l    = function(j) t[ (q+2+(1:m)-1)*p + j ,k] 
		get_lpsi = function(j) log( t[ (q+m+3-1)*p + j ,k] ) 
		
		beta = unlist( lapply( 1:p , get_beta ) ) 
		lsig = unlist( lapply( 1:p , get_lsig ) ) 
		lnu  = unlist( lapply( 1:p , get_lnu  ) ) 
		l    = unlist( lapply( 1:p , get_l    ) ) 
		lpsi = unlist( lapply( 1:p , get_lpsi ) ) 
		
		l = as.vector( t(matrix( l , nrow=m )) ) 
		
		c( beta , lsig , lnu , l , lpsi ) 
	} 
	matrix( unlist( lapply( 1:ncol(t) , get_column ) ) , ncol=ncol(t) ) 
} 

# converts a matrix GPU parameters to a matrix of R parameters  
# each column is a new parameter 
t_from_gpu = function(t,q=4,p=360,m=5) 
{ 
	t = as.matrix(t) 
	get_column = function(k) 
	{ 
		beta = matrix( t[ 1:(q*p) ,k] , nrow=q , ncol=p ) 
		nu = exp( t[ p*(q+1) + 1:p ,k] ) 
		sig = exp( t[ p*q + 1:p ,k] ) 
		l = matrix( t[ p*(q+2) + 1:(p*m) ,k] , nrow=p , ncol=m )  
		psi = exp( t[ p*(q+2+m) + 1:p ,k] ) 
		
		get_dim = function(j) c( beta[,j] , nu[j] , sig[j] , l[j,] , psi[j] ) # extract a row 
		
		as.vector( t( matrix( unlist( lapply( 1:p , get_dim ) ) , ncol=p ) ) ) 
	} 
	
	matrix( unlist( lapply( 1:ncol(t) , get_column ) ) , ncol=ncol(t) ) 
} 

get_a_bootstrap = function(y,x,formula=~ .,m=5,nu0=1.5,...) 
{ 
	n = nrow(y) 
	idx = sample( 1:n , n , replace=T ) 
	lt_prototype_est( y[idx,] , x[idx,] , formula , m , nu0 , ... ) 
} 

compress_boots = function( boot_list )
{
        ncol = length(boot_list)
        matrix( unlist(boot_list) , ncol=length(boot_list) )
}

get_many_bootstraps = function(y,x,n_threads=16,n_bootstraps=100,formula=~ .,m=5,nu0=1.5,gpu=F,...) 
{ 
	# clean data 
	n = nrow(y) 
	if( n != nrow(x) ){ print("y & x must have an equal number of rows!") ; return(NA) } 
	no_na_idx = (1:n)[ rowSums( is.na(y) ) + rowSums( is.na(x) ) + rowSums( is.infinite(as.matrix(y)) ) + rowSums( is.infinite(as.matrix(x)) ) == 0 ] 
	y = y[ no_na_idx ,] 
	cnx = colnames(x) 
	x = as.data.frame(x[ no_na_idx ,]) 
	colnames(x) = cnx 
	# get to work  
	f = function( i , par ) 
	{ 
		source("fmwc_regression.R") 
		ncx = colnames(par$x) 
		par$x = as.data.frame( par$x[ par$idx[[i]] ,] ) 
		colnames(par$x) = ncx 
		lt_prototype_est( par$y[ par$idx[[i]] ,] , par$x , par$formula , par$m , par$nu0 , par$fit0 )  
	} 
	par = list() 
	par$y = y 
	par$x = x 
	par$formula = formula 
	par$m = m 
	par$nu0 = nu0 
	par$fit0 = lt_prototype_est( par$y , par$x , par$formula , par$m , par$nu0 ) 
	par$idx = lapply( 1:n_bootstraps , function(i) sample( 1:nrow(y) , nrow(y) , replace=T ) ) 
	cl = makeCluster( n_threads ) 
	out = parLapply( cl , 1:n_bootstraps , f , par ) 
	stopCluster( cl ) 
	out = compress_boots( out ) 
	if( gpu ) 
	{ 
		npar = nrow(out) 
		q = ncol( model.matrix( formula , x ) ) 
		p = ncol(y) 
		gpu_optimize = function(i) 
		{ 
			print( paste0( "Boot " , i ) ) 
			t_gpu = t_to_gpu( out[,i] , q,p,m ) 
			ncx = colnames(par$x) 
			par$x = as.data.frame( par$x[ par$idx[[i]] ,] ) 
			colnames(par$x) = ncx 
			t_gpu = fmwc_gpu_mle( par$y[ par$idx[[i]] ,] , par$x , t_gpu , formula , m , ... )$mle
			t_from_gpu( t_gpu , q,p,m ) 
		} 
		out = matrix( unlist( lapply( 1:n_bootstraps , gpu_optimize ) ) , nrow=npar ) 
	} 
	out 
} 

gpu_estimate = function(y,x,formula=~ .,m=5,nu0=1.5,...) 
{ 
	t0 = lt_prototype_est( y , x , formula , m , nu0 ) 
	q = ncol( model.matrix( formula , x ) ) 
	p = ncol(y) 
	t_gpu = t_to_gpu( as.vector(t0) , q,p,m ) 
	t_gpu = fmwc_gpu_mle( y , x , t_gpu , formula , m , ... )$mle 
	t_from_gpu( t_gpu , q,p,m ) 
} 

get_significance = function( boots , p=360 , alpha=0.95 , x=NULL , formula=~. , m=NULL ) 
{ 
	nboots = ncol(boots) 
	sign_sums = rowSums( sign(boots) ) 
	sig = abs(sign_sums) / nboots > alpha 
	out = sign( sign_sums ) * sig 
	out = matrix( out , nrow=p ) # headers correspond to the same as an initial estimated matrix  
	if( (! is.null(x)) & (! is.null(formula)) & (! is.null(m)) ) 
	{ 
		n1 = colnames( model.matrix(formula,x) ) 
		n2 = c( "sig" , "nu" ) 
		n3 = paste0( "l" , 1:m ) 
		n4 = "psi" 
		colnames(out) = c( n1 , n2 , n3 , n4 ) 
	} 
	out 
} 








