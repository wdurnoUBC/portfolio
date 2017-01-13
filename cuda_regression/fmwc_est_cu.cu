
/*
* Author: W. Evan Durno 
* Written April 2016 
* All rights reserved 
*/ 

#include <stdio.h> 
#include <float.h> 

////////////////////////////////////////////////////////////////////////////////////// external facing headers 

extern "C" { 

// GPU-accelerated multivariate regression with a factor model and floor student-t marginals via gaussian copula 
// all matrices are stores in column-major order 
// y : n X p matrix, counts data to be regressed 
// x : n X q matrix, design matrix   
// t : tn X K matrix, parameters, each column is a different parameter
// specifically, the order of parameters in one t column is beta_{q X p} , lsig_{p} , lnu_{p} , l_{p X m} , lpsi_{p} , always column-major order  
// m : the number of factors  
// p is assumed > 32, otherwise use of this software is not motivated 
// returns out : K-length vector of sample likelihoods 
void log_lik_cu ( float *y , float *x , float *t , int n , int p , int q , int tn , int K , int m , int n_iter , int seed , float *out ) ; 

} // end extern "C" 

////////////////////////////////////////////////////////////////////////////////////// local headers  

// stores device locations of major parameter components for the first parameter t 
// later elements can be accessed via column shifts 
struct params 
{ 
	float *beta ; 
	float *lsig ; 
	float *lnu ; 
	float *l ; 
	float *lpsi ; 
}; 

// Extracts parameters elements from a single parameter vector  
void construct_params ( float *t , struct params *a , int p , int q , int m ) ; 

// NOT USED   
// constructs I + L' inv(Psi) L 
// out : m X m X K matrix  
// threads : m X m X K  
__global__ void f2_make_inner_sig ( float *l , float *psi , float *out , int p , int m , int K , int tn ) ; 

// constructs F_M( y ) 
// mode : in { 1 , 2 , 3 } , indicating different parts of the argument domain  
// plus : an amount to add to y, either 0 or 1   
// out : n X p X K matrix   
// threads : n X p X K    
__global__ void f3_marginal_cdf ( float *y , float *x , float *beta , float *lsig , float *lnu , float *out , int n , int p , int q , int K , int tn , int mode , float plus ) ; 

// constructs diag( L L' + Psi )  
// out : p X K matrix  
// threads : p X K  
__global__ void f4_diag_sig ( float *l , float *psi , float *out , int p , int m , int K , int tn ) ; 

// NOT USED  
// constructs log det( L L' + Psi ) 
// out : K-length vector of matrix determinants  
__global__ void f5_ldet ( float *lpsi , float **ri , float *out , int m , int p , int K , int tn ) ; 

// NOT_USD  
// constructs inv( I + L' inv(Psi) L ) 
// in : product of f2 
// w : 4 X m X m X K matrix, working space 
// out : m X m X K matrix in out , but also m X m X K matrix in r 
// threads : K  
__global__ void f6_inv_inner_sig ( float *f2 , float *out , int m , int K , float *w , float **r ) ; 

// constructs log f_M(y) 
// in : f3( y ) , f3( y+1 ) 
// out : n X p X K matrix 
// threads : n X p X K  
__global__ void f7_lmarginal_pdf ( float *f3 , float *f31 , float *out , int n , int p , int K ) ; 

// NOT USED  
// constructs inv(Psi) - inv(Psi) L inv( I + L' inv(Psi) L ) L' inv(Psi) = inv(Sigma) 
// in : product of f6 
// out : p X p X K matrix   
// threads : p X p X K 
__global__ void f8_inv_sig ( float *f6 , float *l , float *lpsi , float *out , int p , int m , int K , int tn ) ; 

// constructs Sum_j log f_M( y_{ij} ) 
// in : product of f7 
// out : n X K matrix  
// threads n X K 
__global__ void f9_sum_lpdfs ( float *f7 , float *out , int n , int p , int K ) ; 

// constructs F_N^{-1}( F_M( y+1 ) ) 
// in : f3( y ) and product of f4 
// out : n X p X K matrix  
// threads : n X p X K  
__global__ void f10_F_N_inv ( float *f3 , float *f4 , float *out , int n , int p , int K ) ; 

// constructs log f_{N_p} ( f10 ) 
// out : n X K matrix  
// threads : n X K   
__global__ void f11_lpmnorm ( float *f10 , float *f101 , float *l , float *lpsi , float *out , int n , int p , int m , int K , size_t seed , int n_iter , float *w ) ; 

// constructs log_lik( t ; y , x ) 
// out : K-length vector   
// threads : K  
__global__ void f12_sum_log_likes ( float *f11 , float *f9 , float *out , int n , int K ) ;  

////////////////////////////////////////////////////////////////////////////////////// host implementations 

void log_lik_cu ( float *y , float *x , float *t , int n , int p , int q , int tn , int K , int m , int n_iter , int seed , float *out ) 
{ 
	size_t n_cores = 32 ;
        if( n_cores * 65536 < max( p*p*K , n*p*K ) ) 
                n_cores = max( p*p*K , n*p*K )/65536 + 1 ; 
        if( n_cores >= 1024 ) 
                n_cores = 1023 ; 
        if( n_cores * 65536 < max( p*p*K , n*p*K ) ) 
                fprintf( stderr , "CUDA WARNING: insufficient threads!\n" ) ; 
	
	cudaError_t status = cudaSuccess ; 
	
	// initialize streams 
	// s[0] : NOT USED  
	// s[1] : NOT USED  
	// s[2] : f7 - f9, f7 requires s[7:12]  
	// s[3] : f10(y), f10 requires s[7:9] as well as f4 via event[6]  
	// s[4] : f4 - f10(y+1), f10 requires s[10:12] 
	// s[5] : f11 - f12, f11 requires s[0], s[1], s[3], s[4], and f12 requires s[2]  
	// s[6] : NOT USED  
	// s[7] : f3(y,1) 
	// s[8] : f3(y,2) 
	// s[9] : f3(y,3) 
	// s[10] : f3(y+1,1) 
	// s[11] : f3(y+1,2) 
	// s[12] : f3(y+1,3) 
	cudaStream_t stream[13] ; 
	int i ; 
	for( i = 0 ; i < 13 && status == cudaSuccess ; i++ ) 
		status = cudaStreamCreate( stream + i ) ; 
	
	// initialize events 
	// event[i] marks the completion of stream[i] 
	// except event[6] which marks the completion of f4 in s[4]  
	cudaEvent_t event[13] ; 
	for( i = 0 ; i < 13 && status == cudaSuccess ; i++ ) 
		status = cudaEventCreate( event + i ) ; 
	
	// represent device memory 
	float *d_y = NULL ; 
	float *d_x = NULL ; 
	float *d_t = NULL ; 
	float *d_f3 = NULL ; 
	float *d_f31 = NULL ; 
	float *d_f4 = NULL ; 
	float **d_ri = NULL ; 
	float *d_f7 = NULL ; 
	float *d_f9 = NULL ; 
	float *d_f10 = NULL ; 
	float *d_f101 = NULL ; 
	float *d_f11 = NULL ; 
	float *d_f11w = NULL ; 
	float *d_f12 = NULL ; 
	
	// allocate device memory 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_y , n * p * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_x , n * q * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_t , tn * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f3 , n * p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f31 , n * p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f4 , p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_ri , K * sizeof(float*) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f7 , n * p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f9 , n * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f10 , n * p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f101 , n * p * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f11 , n * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f11w , m * n * K * sizeof(float) ) ; 
	if( status == cudaSuccess ) 
		status = cudaMalloc( &d_f12 , K * sizeof(float) ) ; 
	
	// populate device memory 
	if( status == cudaSuccess ) 
		status = cudaMemcpy( d_y , y , n * p * sizeof(float) , cudaMemcpyHostToDevice ) ; 
	if( status == cudaSuccess ) 
		status = cudaMemcpy( d_x , x , n * q * sizeof(float) , cudaMemcpyHostToDevice ) ; 
	if( status == cudaSuccess ) 
		status = cudaMemcpy( d_t , t , tn * K * sizeof(float) , cudaMemcpyHostToDevice ) ; 
	
	size_t free_mem , total_mem ; 
	if( status == cudaSuccess ) 
		status = cudaMemGetInfo( &free_mem, &total_mem ) ; 
	if( status == cudaSuccess ) 
		fprintf( stderr , "Device free mem: %lu, total mem: %lu, remaining: %f%%\n" , free_mem , total_mem , ((float) free_mem)/((float) total_mem) ) ; 
	
	// identify parameter elements  
	struct params a ; 
	construct_params ( d_t , &a , p , q , m ) ; 
	
	// count threads 
	size_t t3 = n * p * K ; 
	size_t t4 = p * K ; 
	size_t t7 = n * p * K ; 
	size_t t9 = n * K ; 
	size_t t10 = n * p * K ; 
	size_t t11 = n * K ; 
	size_t t12 = K ;  
	
	///////////////// run and schedule kernels 
	
	// stream[7:9] 
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[7] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f3 , n , p , q , K , tn , 1 , 0.0f ) ;  
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[8] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f3 , n , p , q , K , tn , 2 , 0.0f ) ; 
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[9] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f3 , n , p , q , K , tn , 3 , 0.0f ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[7] , stream[7] ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[8] , stream[8] ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[9] , stream[9] ) ; 
	
	// stream[10:12] 
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[10] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f31 , n , p , q , K , tn , 1 , 1.0f ) ; 
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[11] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f31 , n , p , q , K , tn , 2 , 1.0f ) ; 
	if( status == cudaSuccess ) 
		f3_marginal_cdf <<< t3 / n_cores + 1 , n_cores , 0 , stream[12] >>> ( d_y , d_x , a.beta , a.lsig , a.lnu , d_f31 , n , p , q , K , tn , 3 , 1.0f ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[10] , stream[10] ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[11] , stream[11] ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[12] , stream[12] ) ; 
	
	// stream[2] 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[7] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[8] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[9] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[10] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[11] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[2] , event[12] , 0 ) ; 
	if( status == cudaSuccess ) 
		f7_lmarginal_pdf <<< t7 / n_cores + 1 , n_cores , 0 , stream[2] >>> ( d_f3 , d_f31 , d_f7 , n , p , K ) ; 
	if( status == cudaSuccess ) 
		f9_sum_lpdfs <<< t9 / n_cores + 1 , n_cores >>> ( d_f7 , d_f9 , n , p , K ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[2] , stream[2] ) ; 
	
	// stream[4] 
	if( status == cudaSuccess ) 
		f4_diag_sig <<< t4 / n_cores + 1 , n_cores , 0 , stream[4] >>> ( a.l , a.lpsi , d_f4 , p , m , K , tn ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[6] , stream[4] ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[4] , event[10] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[4] , event[11] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[4] , event[12] , 0 ) ; 
	if( status == cudaSuccess ) 
		f10_F_N_inv <<< t10 / n_cores + 1 , n_cores , 0 , stream[4] >>> ( d_f31 , d_f4 , d_f101 , n , p , K ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[4] , stream[4] ) ; 
	
	// stream[3]  
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[3] , event[6] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[3] , event[7] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[3] , event[8] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[3] , event[9] , 0 ) ; 
	if( status == cudaSuccess ) 
		f10_F_N_inv <<< t10 / n_cores + 1 , n_cores , 0 , stream[3]  >>> ( d_f3 , d_f4 , d_f10 , n , p , K ) ; 
	if( status == cudaSuccess ) 
		status = cudaEventRecord( event[3] , stream[3] ) ; 
	
	// stream[5] 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[5] , event[3] , 0 ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[5] , event[4] , 0 ) ; 
	if( status == cudaSuccess ) 
		f11_lpmnorm <<< t11 / n_cores + 1 , n_cores , 0 , stream[5] >>> ( d_f10 , d_f101 , a.l , a.lpsi , d_f11 , n , p , m , K , seed , n_iter , d_f11w ) ; 
	if( status == cudaSuccess ) 
		status = cudaStreamWaitEvent( stream[5] , event[2] , 0 ) ; 
	if( status == cudaSuccess ) 
		f12_sum_log_likes <<< t12 / n_cores + 1 , n_cores >>> ( d_f11 , d_f9 , d_f12 , n , K ) ; 
	
	///////////////// finished running and scheduling kernels 
	
	// extract data from device 
	if( status == cudaSuccess ) 
		status = cudaMemcpy( out , d_f12 , K * sizeof(float) , cudaMemcpyDeviceToHost ) ; 
	
	// delete streams 
	for( i = 0 ; i < 13 && status == cudaSuccess ; i++ ) 
		status = cudaStreamDestroy( stream[i] ) ; 
	
	// delete events 
	for( i = 0 ; i < 13 && status == cudaSuccess ; i++ ) 
		status = cudaEventDestroy( event[i] ) ; 
	
	// check for errors 
	if( status != cudaSuccess ) 
		fprintf( stderr , "CUDA ERROR: %s\n" , cudaGetErrorString(status) ) ;  
	
	// free device memory 
	cudaFree( d_y ) ; 
	cudaFree( d_x ) ; 
	cudaFree( d_t ) ; 
	cudaFree( d_f3 ) ; 
	cudaFree( d_f31 ) ; 
	cudaFree( d_f4 ) ; 
	cudaFree( d_ri ) ; 
	cudaFree( d_f7 ) ; 
	cudaFree( d_f9 ) ; 
	cudaFree( d_f10 ) ; 
	cudaFree( d_f101 ) ; 
	cudaFree( d_f11 ) ; 
	cudaFree( d_f12 ) ; 
	cudaFree( d_f11w ) ; 
	
	return ; 
} 

void construct_params ( float *t , struct params *a , int p , int q , int m ) 
{ 
	a->beta = t ; 
	a->lsig = t + p*q ; 
	a->lnu = t + p*(q + 1) ; 
	a->l = t + p*(q + 2) ; 
	a->lpsi = t + p*(q + 2 + m) ; 
} 

////////////////////////////////////////////////////////////////////////////////////// device implementations 

__global__ void f2_make_inner_sig ( float *l , float *psi , float *out , int p , int m , int K , int tn ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= m * m * K ) 
		return ; 
	int k = i / (m*m) ; 
	int col = (i - k*m*m) / m ; 
	int row = i - k*m*m - col*m ; 
	out += i ; 
	*out = 0.0f ; 
	for( i = 0 ; i < p ; i++ ) 
		*out += expf( -psi[ k*tn + i ]) * l[ k*tn + p*row + i ] * l[ k*tn + p*col + i ] ; 
	if( row == col ) 
		*out += 1.0f ; 
} 

__device__ double beta( float a , float b )
{
        return exp( lgammaf(a) + lgamma(b) - lgamma(a+b) ) ;
}

__device__ double beta_sr ( float x , float a , float b , float i )
{
        return expf( (a+i)*logf(x) + b*logf(1-x) - logf(a+i) + lgammaf(a+i+b) - lgammaf(a+i) - lgammaf(b) ) ;
}

__device__ void pt_beta_reductions ( float *x , float nu ) 
{
        int flag = ( (*x)*(*x) > nu )? 1 : 0 ;
        float y = ( flag )? nu/( (*x)*(*x) + nu) : 1.0f - 1.0f/( 1.0f + ((*x)/nu)*(*x) ) ;
        float a = ( flag )? 0.5f*nu : 0.5f ;
        float b = ( flag )? 0.5f : 0.5f*nu ;
        float out = 0.0f ;
        int i = 0 ;
        for( i = 0 ; i < 20 ; i++ ) 
        { 
                out += beta_sr( y , a , b , (float) i ) ;
        } 
        out = ( flag )? 1.0f - 0.5f*out : 1.0f - 0.5f*(1.0f - out) ;
        out = ( *x < 0.0 )? 1.0f - out : out ;
        *x = out ;
}

__device__ void pt_alg395 ( float *x , float nu )
{
        float t = (*x) * (*x) ;
        float y = t/nu ;
        float b = 1.0f + y ;
        y = ( y > 1e-6f )? logf(b) : y ;
        float a = nu - 0.5 ;
        b = 48.0f * a * a ;
        y = a * y ;
        y = (((((-0.4f*y-3.3f)*y - 24.0f)*y - 85.5f)/(0.8f*y*y + 100.0f + b) + y + 3.0f)/b + 1.0f)*sqrtf(y) ;
        y = normcdff( y ) ;
        *x = ( *x > 0.0f )? y : 1.0f - y ;
}

__device__ void pt_normal ( float *x ) 
{
        *x = normcdff( *x ) ;
}

__global__ void f3_marginal_cdf ( float *y , float *x , float *beta , float *lsig , float *lnu , float *out , int n , int p , int q , int K , int tn , int mode , float plus ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= n * p * K ) 
		return ; 
	int k = i / (p*n) ; 
	int col = (i - k*p*n) / n ; // dimension  
	int row = i - k*p*n - col*n ; // sample  
	if( mode == 1 ) 
	{ 
		if( expf( lnu[ tn*k + col ] ) <= 1e5f ) // must satisfy to proceed: nu > 1e5f 
			return ; 
		y += n*col + row ; 
		out += i ; 
		if( *y + plus <= 0.0f ) // if y is zero, all forms of f3 return 0.0f. We do this under the 'mode ==' test to avoid write collisions. 
		{ 
			*out = 0.0f ; 
			return ; 
		} 
		float sig = expf( lsig[ tn*k + col ] ) ; 
		float mu = 0.0f ; 
		for( i = 0 ; i < q ; i++ ) 
			mu += x[ i*n + row ] * beta[ k*tn + col*q + i ] ; 
		*out = logf((*y) + plus)/sig - mu ; 
		pt_normal( out ) ;  
	} 
	if( mode == 2 ) 
	{ 
                float nu = expf( lnu[ tn*k + col ] ) ;
                if( nu > 1e5f ) // must satisfy to proceed: nu <= 1e5f  
                        return ;
                y += n*col + row ;
                float sig = expf( lsig[ tn*k + col ] ) ;
                float mu = 0.0f ; 
		int j ; 
                for( j = 0 ; j < q ; j++ )
                        mu += x[ j*n + row ] * beta[ k*tn + col*q + j ] ;
                float tmp = logf((*y) + plus)/sig - mu ;
                if( ! ( nu > 200.0f || ( tmp*tmp < nu && nu > 4.0f ) ) ) 
                        return ;
                out += i ;
                if( *y + plus <= 0.0f ) 
                {
                        *out = 0.0f ;
                        return ;
                }
                *out = tmp ;
                pt_alg395 ( out , nu ) ;
	} 
	if( mode == 3 ) 
	{ 
                float nu = expf( lnu[ tn*k + col ] ) ;
                if( nu > 1e5f )  
                        return ;
                y += n*col + row ;
                float sig = expf( lsig[ tn*k + col ] ) ;
                float mu = 0.0f ; 
		int j ; 
                for( j = 0 ; j < q ; j++ )
                        mu += x[ j*n + row ] * beta[ k*tn + col*q + j ] ;
                float tmp = logf((*y) + plus)/sig - mu ;
                if( nu > 200.0f || ( tmp*tmp < nu && nu > 4.0f ) )       
                        return ;
                out += i ;
                if( *y + plus <= 0.0f ) 
                {
                        *out = 0.0f ;
                        return ;
                }
                *out = tmp ;
                pt_beta_reductions ( out , nu ) ; 
	} 
} 

__global__ void f4_diag_sig ( float *l , float *psi , float *out , int p , int m , int K , int tn ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= p * K ) 
		return ; 
	int k = i / p ; 
	int row = i - k*p ; // dimension  
	out += i ; // row-th row in the k-th vector  
	*out = expf( psi[ k*tn + row ] ) ; 
	for( i = 0 ; i < m ; i++ ) 
		*out += l[ k*tn + i*p + row ] * l[ k*tn + i*p + row ] ;  
} 

__global__ void f5_ldet ( float *lpsi , float **ri , float *out , int m , int p , int K , int tn ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= K ) 
		return ; 
	int k = i ; 
	float *invR = ri[i] ; 
	out += i ; 
	*out = 0.0f ; 
	for( i = 0 ; i < p ; i++ ) 
		*out += lpsi[ k*tn + i ] ; // returns on log scale  
	for( i = 0 ; i < m ; i++ ) 
		*out -= logf( fabs(invR[ i*m + i ]) ) ; // algebraically correct and numerically stable (avoids squaring)   
} 

__device__ void copyFloats ( float *dest , float *src , int n )
{
        float *fin = dest + n ;
        while( dest < fin )
        {
                *dest = *src ;
                dest++ ;
                src++ ;
        }
}

// single-threaded matrix product 
// utilizes a single cuda-core, meant for mass products 
// x : nXn matrix 
// y : nXn matrix 
// w : nXn matrix, working space 
// returns in w 
__device__ void mat_prod_serial ( float *x , float *y , int n , float *w )
{
        float *fin = x + n*n ;
        float *t1 = NULL ;
        float *t2 = NULL ;
        int i , j ;
        for( j = 0 ; j < n ; j++ )
        {
                for( i = 0 ; i < n ; i++ )
                {
                        t1 = x + i ; // i-th row of x  
                        t2 = y + j*n ; // j-th column of y 
                        *w = 0.0f ;
                        while( t1 < fin )
                        {
                                *w += (*t1) * (*t2) ;
                                t1 += n ;
                                t2 ++ ;
                        }
                        w++ ;
                }
        }
}

// Householder reflection matrix 
// Creates the (nx-nv+1)-th Householder reflection matrix for QR decomposition 
// x : output matrix, nx X nx 
// v : vector of length nv  
// requires nv <= nx  
__device__ void construct_reflection_matrix ( float *x , float *v , int nx , int nv )
{
        int N = nx - nv ;
        int i , j ;
        for( i = 0 ; i < nx ; i++ )
        {
                for( j = 0 ; j < nx ; j++ )
                {
                        if( i == j )
                                x[ i + nx*i ] = 1.0f ;
                        else
                                x[ i + nx*j ] = 0.0f ;
                        if( i >= N && j >= N ) // less 2 X v's outer product  
                        {
                                x[ i + nx*j ] -= 2.0f * v[ i-N ] * v[ j-N ] ;
                        }
                }
        }
}

// invert upper-triangular r_{n X n} into ri with back fitting  
__device__ void invert_upper_serial ( float *r , int n , float *ri )
{
        int i , j ; // row and column of ri respectively 
        int k ;
        for( j = 0 ; j < n ; j++ )
        {
                for( i = n-1 ; i >= 0 ; i-- )
                {
                        if( i > j ) // lower quadrant 
                                ri[ i + j*n ] = 0.0f ;
                        else if( i == j )
                                ri[ i + j*n ] = 1.0f / r[ i + j*n ] ;
                        else // i < j 
                        {
                                ri[ i + j*n ] = 0.0f ;
                                for( k = j ; k > i ; k-- )
                                        ri[ i + j*n ] -= r[ i + k*n ] * ri[ k + j*n ] ;
                                ri[ i + j*n ] /= r[ i + i*n ] ;
                        }
                }
        }
}

__global__ void f6_inv_inner_sig ( float *f2 , float *out , int m , int K , float *w , float **ri ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= K ) 
		return ; 
	out += i*m*m ; 
	if( m == 1 ) 
	{ 
		*out = 1.0f / (*out) ; 
		return ; 
	} 
        int work_n = 4*m*m ; // total working space size per thread  
        float *qt = w + i * work_n ; // transpose of Q matrix; product of QR decomposition 
        float *qi = w + i*work_n + m*m ; // Q_i workingspace for QR decomp 
        ri[i] = w + i*work_n + 2*m*m ; // space for R^{-1} after QR decomp 
        float *w2 = w + i*work_n + 3*m*m ; // secondary working space 
        float *v = w2 ; // different name for same space, stands in for a vector. Optimizer should remove at compile 
        float mag = -1.0f ; // magnitude 
	
	// copy f1 to out 
	f2 += i*m*m ; 
	copyFloats ( out , f2 , m*m ) ; 
	
        // set qt to identity  
        int ii , j ;
        for( ii = 0 ; ii < m ; ii++ )
        { 
                for( j = 0 ; j < m ; j++ )
                {
                        if( ii == j )
                                qt[ ii + j*m ] = 1.0f ;
                        else
                                qt[ ii + j*m ] = 0.0f ;
                }
        } 
	
	// QR decomposition via Householder reflections 
        for( ii = 0 ; ii < m ; ii++ ) 
        {
                // calc rotation vector  
                for( j = 0 ; j < m - ii ; j++ )
                        v[j] = out[ ii + j + ii*m ] ;
                mag = 0.0f ;
                for( j = 0 ; j < m - ii ; j++ )
                        mag += v[j]*v[j] ;
                mag = sqrtf(mag) ;
                v[0] += copysignf( mag , v[0] ) ;
                mag = 0.0f ;
                for( j = 0 ; j < m - ii ; j++ )
                        mag += v[j]*v[j] ;
                mag = sqrtf( mag ) ;
                for( j = 0 ; j < m - ii ; j++ )
                        v[j] /= mag ;

                construct_reflection_matrix( qi , v , m , m-ii ) ;

                // update m = Qi m 
                mat_prod_serial ( qi , out , m , w2 ) ; // optional todo : force zeros below i-th entry of i-th column 
                copyFloats ( out , w2 , m*m ) ; // m stores r 

                // update qt = qi qt 
                mat_prod_serial ( qi , qt , m , w2 ) ;
                copyFloats ( qt , w2 , m*m ) ;
        }

        // Backfit R to I, producing a numerically stable inverse 
        invert_upper_serial ( out , m , ri[i] ) ;

        // Matrix product R^{-1} Q^T produces the inverse of m  
        mat_prod_serial ( ri[i] , qt , m , out ) ;
} 

__global__ void f7_lmarginal_pdf ( float *f3 , float *f31 , float *out , int n , int p , int K ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= n*p*K ) 
		return ; 
	int k = i / (n*p) ; 
	int col = (i - k*n*p) / n ; 
	int row = i - k*n*p - col*n ; 
	out += i ; // k-th matrix, row-th row, col-th col 
	*out = fabsf( f31[ k*n*p + col*n + row ] - f3[ k*n*p + col*n + row ] ) ; // I expect round-offs in the tail 
	*out = ( *out <= 0.0f ) ? sqrtf(FLT_MIN) : *out ; // just in case 
	*out = logf(*out) ;  
} 

__global__ void f8_inv_sig ( float *f6 , float *l , float *lpsi , float *out , int p , int m , int K , int tn ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= p*p*K ) 
		return ; 
	int k = i / (p*p) ; 
	int col = (i - k*p*p) / p ; 
	int row = i - k*p*p - col*p ; 
	out += i ; // k-th matrix, col-th col, row-th row 
	int j ; 
	*out = 0.0f ; 
	for( i = 0 ; i < m ; i++ ) 
	{ 
		for( j = 0 ; j < m ; j++ ) 
			*out += l[ k*tn + j*p + row ] * f6[ k*m*m + j*m + i ] * l[ k*tn + i*p + col ] ; 
	} 
	*out *= expf( -lpsi[k*tn + row] - lpsi[k*tn + col] ) ; 
	*out = ( row == col ) ? expf( -lpsi[ k*tn + row ] ) - (*out) : -(*out) ; 
} 

__global__ void f9_sum_lpdfs ( float *f7 , float *out , int n , int p , int K ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= n*K ) 
		return ; 
	int k = i / n ; 
	int row = i - k*n ; 
	out += i ; // k-th vector, row-th row  
	*out = 0.0f ; 
	for( i = 0 ; i < p ; i++ ) 
		*out += f7[ k*n*p + i*n + row ] ;  
} 

__global__ void f10_F_N_inv ( float *f3 , float *f4 , float *out , int n , int p , int K ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= n * p * K ) 
		return ; 
	int k = i / (n*p) ; 
	int col = (i - k*n*p) / n ; 
	int row = i - k*n*p - col*n ; 
	out += i ; // k-th matrix, col-th col, row-th row 
	if( f3[ k*n*p + col*n + row ] <= 0.0f ) 
		*out = -FLT_MAX ; 
	else if( f3[ k*n*p + col*n + row ] >= 1.0f ) 
		*out = FLT_MAX ; 
	else 
		*out = sqrtf( f4[ k*p + col ] ) * normcdfinvf( f3[ k*n*p + col*n + row ] ) ;  
} 

// Add log scale values and return on log scale 
// Used to avoid under/over-flows   
// returns log( a + b )  
__device__ float logSum ( float a , float b ) 
{ 
	if( a < b ) // exponents can't be allowed to get too large  
	{ 
		float t = a ; 
		a = b ; 
		b = t ; 
	} 
	return a + logf( 1.0f + expf( b - a ) ) ;  
} 

// Subtract log scale values from one another 
// returns log( a - b )   
__device__ float logSubtract( float a , float b ) 
{ 
	return a + logf( 1.0f - expf( b - a ) ) ; 
} 

// a mod b  
__device__ size_t mod( size_t a , size_t b ) 
{ 
	return a - b*(a/b) ; 
} 

__device__ float generate_unif_lcg( size_t *seed ) 
{ 
	*seed = mod( 1103515245*(*seed) + 12345 , 2147483648 ) ; 
	return ((float) ((*seed)) + 10000) / 2147503648.0f ; // = 2147483648 + 20000 ; do not return 0 or 1 
} 

__device__ float truncate ( float x ) 
{ 
	return copysignf( floorf( fabsf(x) ) , x ) ; 
} 

// Returns log( normal_cdf(x) )  
__device__ float log_norm_cdf ( float x ) 
{ 
	// Catch boundary cases  
	if( x >= 1e37 ) 
		return 0.0f ;  
	if( x <= -1e37 ) 
		return -1.0f/0.0f ; 
	// Use pre-written CUDA software when possible  
	if( x <= 5.657f && x >= -5.657f ) // sqrt(32)  
		return logf(normcdff(x)) ; 
	// else : I borrowed this from R's prorm.c, references were ungiven for algorithm content  
	float p[6] = {
        0.21589853405795699f ,
        0.1274011611602473639f ,
        0.022235277870649807f ,
        0.001421619193227893466f ,
        2.9112874951168792e-5f ,
        0.02307344176494017303f }; 
	float q[5] = { 
        1.28426009614491121f ,
        0.468238212480865118f ,
        0.0659881378689285515f ,
        0.00378239633202758244f ,
        7.29751555083966205e-5f }; 
	float xsq = 1/(x*x) ; 
	float xnum = p[5]*xsq ; 
	float xden = xsq ; 
	int i ; 
	for( i = 0 ; i < 4 ; i++ ) 
	{ 
		xnum = ( xnum + p[i] ) * xsq ; 
		xden = ( xden + q[i] ) * xsq ; 
	} 
	float tmp = xsq * ( xnum + p[4] ) / ( xden + q[4] ) ; 
	tmp = ( 0.3989422804014327 - tmp ) / fabsf(x) ; 
	
//	xsq = truncate( x * 16.0f ) / 16.0f ; // modified from original code   
	xsq = truncate( x * 64.0f ) / 64.0f ; 
	float del = (x - xsq) * (x + xsq) ; 
	
	if( x < 0.0f ) 
		return ( -xsq * xsq * 0.5f ) + ( -del * 0.5f ) + logf(tmp) ; 
	return logf( 1.0f - expf( -xsq * xsq * 0.5f )*expf( -del * 0.5f ) * tmp ) ; 
} 

__global__ void f11_lpmnorm ( float *f10 , float *f101 , float *l , float *lpsi , float *out , int n , int p , int m , int K , size_t seed , int n_iter , float *w ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= n * K ) 
		return ; 
	int k = i / n ; 
	int row = i - k*n ; 
	out += i ; // k-th vector, row-th row 
	w += m*i ; 
	size_t local_seed = seed + i ; 
	float lo, hi, mu , tmp , swp ; 
	float sqMin = sqrt(FLT_MIN) ; 
	*out = FLT_MIN ; 
	int j , kk ; 
	for( i = 0 ; i < n_iter ; i++ ) // MC-integral, E[ P( N in [a,b] | F ) ] = P( N in [a,b] ) 
	{ 
		for( kk = 0 ; kk < m ; kk++ ) 
			w[kk] = normcdfinvf( generate_unif_lcg( &local_seed ) ) ; // generate a standard normal 
		tmp = 0.0f ; 
		for( j = 0 ; j < p ; j++ ) 
		{ 
			mu = 0.0f ; 
			for( kk = 0 ; kk < m ; kk++ ) 
				mu += l[ k*p*m + kk*p + j ] * w[kk] ; 
			lo = (f10[ k*n*p + j*n + row ] - mu) * fmaxf( sqMin , expf( -lpsi[ k*p + j ] ) ); 
			hi = (f101[ k*n*p + j*n + row ] - mu) * fmaxf( sqMin , expf( -lpsi[ k*p + j ] ) ) ; 
			if( hi < lo ) // possible via roundoffs  
			{ 
				swp = lo ; // use as temp variable  
				lo = hi ; 
				hi = swp ; 
			} 
			if( 0.0f < lo ) // both positive likely results in an underflow 
			{ 
				swp = lo ; 
				lo = -hi ; 
				hi = -swp ; 
			} 
			swp = logSubtract( log_norm_cdf(hi) , log_norm_cdf(lo) ) ; 
			if( swp != swp ) // round-off ! 
				tmp += -FLT_MAX/1000.0f ; 
			else 
				tmp += swp ;  
		} 
		if( i == 1 ) 
			*out = tmp ; 
		else  
			*out = logSum( *out , tmp ) ; 
	} 
	*out -= logf( (float) n_iter ) ; 
} 

__global__ void f12_sum_log_likes ( float *f11 , float *f9 , float *out , int n , int K ) 
{ 
	int i = blockIdx.x * blockDim.x + threadIdx.x ; 
	if( i >= K ) 
		return ; 
	out += i ; // i-th entry 
	*out = 0.0f ; 
	int j ; 
	for( j = 0 ; j < n ; j++ ) 
	{ 
		*out += f11[ i*n + j ] + f9[ i*n + j ] ; 
	} 
}  














