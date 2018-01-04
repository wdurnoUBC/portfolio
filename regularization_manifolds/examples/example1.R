
### models:  
# h1 : large model 
# h0 : small unbiased model 
# bias : small biased model 

#### ESTIMATOR DEFINITIONS  

generate_sample = function( n = 1000 ) rnorm(n,1,1) 

h1_solve = function( x ) 
{ 
	n = length(x) 
	mu1 = mean(x) # MLE 
	v1 = var(x)*(n-1)/n # MLE, not unbiased estimate  
	c( mu1 , v1 ) 
}  

h0_solve = function( x ) 
{ 
	f = function(t) mean( dnorm(x, 1+t , abs(1+0.5*t)+0.000001 , log=T ) ) 
	t = optimize( f , lower = -10 , upper = 10 , maximum = T )$objective 
	c( t , 1+t , 1+0.5*t ) # Remark: These are estimates, but their variance will equate due to mere additive differences.  
} 

bias_solve = function( x , bias=0.1 ) 
{ 
	f = function(t) mean( dnorm(x, 1+bias+t , abs(1-bias+t)+0.000001 , log=T ) ) 
	t = optimize( f , lower = -10 , upper = 10 , maximum = T )$maximum  
	c( t , 1+bias+t , 1-bias+t )  
} 

#### VARIANCE DEFINITIONS  

generate_estimates = function( nn , n , solver ) matrix( unlist( lapply( 1:nn , function(i) solver( generate_sample(n) ) ) ) , ncol=nn ) 

h1_sim = function(nn=1000 , n=1000) 
{ 
	theta = generate_estimates( nn , n , h1_solve ) 
	mean( (1 - theta[1,])^2 ) + mean( (1 - theta[2,])^2 ) 
} 

h0_sim = function( nn = 1000 , n = 1000 ) 
{ 
	theta = generate_estimates( nn , n , h0_solve ) 
	c( var( theta[1,] ) , var( theta[2,] ) , var( theta[3,] ) ) 
} 

bias_sim = function( nn = 1000 , n = 1000 , bias = 0.1 ) 
{ 
	theta = matrix( unlist( lapply( 1:nn , function(i) bias_solve( generate_sample(n) , bias ) ) ) , ncol=nn ) 
	mean( (1 - theta[2,])^2 ) + mean( (1 - theta[3,])^2 ) 
} 

#### VISUAL DEMONSTRATION 

illustrate_helper = function( n=100 , nn=100 ) 
{ 
	biases = 0.05 * ( 0:10 ) / 10 
	errors = 1:11 
	for( i in 1:length(biases) ) 
	{ 
		errors[i] = bias_sim( nn , n , biases[i] ) 
	} 
	biases = c( biases , 0.06 ) 
	errors = c( errors , h1_sim( nn , n ) ) 
	
	out = cbind( biases , errors ) 
	colnames( out ) = c( "b" , "e" ) 
	out 
} 

n1000 = illustrate_helper( 1000 , 1000 ) 
plot( e ~ b , n1000 ) 
















