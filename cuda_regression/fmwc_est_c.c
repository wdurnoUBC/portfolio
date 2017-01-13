
#include <stdio.h> 
#include <float.h> 
#include <string.h> 
#include <R.h> 
#include <R_ext/Utils.h> 

extern void log_lik_cu ( float *y , float *x , float *t , int n , int p , int q , int tn , int K , int m , int n_iter , int seed , float *out ) ;  

// Already defined in R.h  
// int isnan(double x) { return x != x; } 
// int isinf(double x) { return !isnan(x) && isnan(x - x); } 

struct static_par 
{ 
	double *y ; 
	double *x ; 
	int *n ; 
	int *p ; 
	int *q ; 
	int *tn ; 
	int *m ; 
	int *n_iter ; 
	int *seed ; 
	// not included: t, K 
}; 

/* 
y : n X p 
x : n X q 
t : tn X K 
nn : n X K 
out : K-length vector 
*/ 
void log_lik_c ( double *y , double *x , double *t , int *n , int *p , int *q , int *tn , int *K , int *m , int *n_iter , int *seed , double *out ) 
{ 
	float *f_y = (float*) Calloc( (*n) * (*p) , float ) ; 
	float *f_x = (float*) Calloc( (*n) * (*q) , float ) ; 
	float *f_t = (float*) Calloc( (*tn) * (*K) , float ) ; 
	float *f_out = (float*) Calloc( *K , float ) ; 
	int i ; 
	for( i = 0 ; i < (*n) * (*p) ; i++ ) 
		f_y[i] = (float) y[i] ; 
	for( i = 0 ; i < (*n) * (*q) ; i++ ) 
		f_x[i] = (float) x[i] ; 
	for( i = 0 ; i < (*tn) * (*K) ; i++ ) // possible to suffer round-offs due to exponentiation 
	{ 
		if( t[i] > 0.0 && t[i] < sqrt(FLT_MIN) ) 
			f_t[i] = (float) sqrt(FLT_MIN) ; 
		else if( t[i] < 0.0 && fabs(t[i]) < sqrt(FLT_MIN) ) 
			f_t[i] = (float) -sqrt(FLT_MIN) ; 
		else if( t[i] > sqrt(FLT_MAX) ) 
			f_t[i] = (float) sqrt(FLT_MAX) ; 
		else if( t[i] < 0.0 && fabs(t[i]) > sqrt(FLT_MAX) ) 
			f_t[i] = (float) - sqrt(FLT_MAX) ; 
		else 
			f_t[i] = (float) t[i] ; 
	} 
	log_lik_cu ( f_y , f_x , f_t , *n , *p , *q , *tn , *K , *m , *n_iter , *seed , f_out ) ; 
	for( i = 0 ; i < (*K) ; i++ ) 
		out[i] = (double) f_out[i] ; 
	Free( f_y ) ; 
	Free( f_x ) ; 
	Free( f_t ) ; 
	Free( f_out ) ; 
} 

// breaks jobs into batches of 'batch_size' parameters at a time  
// A good batch size is often 1000 
// out : K-length vector  
void log_lik_batch ( double *t , int K , struct static_par *p , size_t batch_size , double *out ) 
{ 
	double *fin = t + K * (*(p->tn)) ; 
	while( t < fin ) 
	{ 
		int tmpK = ( batch_size > K ) ? K : batch_size ; 
		log_lik_c( p->y , p->x , t , p->n , p->p , p->q , p->tn , &tmpK , p->m , p->n_iter , p->seed , out ) ; 
		t += batch_size * (*(p->tn)) ; 
		out += batch_size ; 
		K -= batch_size ; 
		p->seed++ ; 
	} 
} 

// returns TRUE if a is nan or inf 
// and if b is not nan or inf   
int real_check ( double a , double b ) 
{ 
	return (isnan(a) || isinf(a)) && (! isnan(b)) && (!  isinf(b)) ; 
} 

// GPU-accelerated FMWC MLE 
// y : n X p matrix of count data 
// x : n X q matrix of count data 
// t : tn-length vector, also output space 
// eps : relative error step size 
// max_it : maximum number of iterations, also used as an output for the number of iterations used  
// test_coefs : test point multiples , length test_coefs_n. Use a default of 1.0, length 1 
// out : log likelihood value, single entry   
void fmwc_gpu_mle ( double *y , double *x , double *t , int *n , int *p , int *q , int *tn , int *m , double *eps , int *batch_size , int *max_it , double *test_coefs , int *test_coefs_n , int *n_iter , int *seed , int *min_it , double *out ) 
{ 
	// Aggregate static args 
	struct static_par a ; 
	a.y = y ; 
	a.x = x ; 
	a.n = n ; 
	a.p = p ; 
	a.q = q ; 
	a.tn = tn ; 
	a.m = m ; 
	a.n_iter = n_iter ; 
	a.seed = seed ; 
	
	double t0_lik = -1.0/0.0 ; // likelihood of initial parameter  
	double *t_test = (double*) Calloc( (*tn) * (*test_coefs_n) , double ) ; // test point parameters  
	double *t_test_lik = (double*) Calloc( (*test_coefs_n) , double ) ; // likelihoods of test point parameters  
	double *t_p1 = (double*) Calloc( (*tn) * (*tn) , double ) ; // stencil parameter, t + Ie   
	double *t_p1_lik = (double*) Calloc( (*tn) , double ) ; // liklihood of stencil parameters, t + Ie  
	double *t_p2 = (double*) Calloc( (*tn) * (*tn) , double ) ; // stencil parameter, t + I*2e  
	double *t_p2_lik = (double*) Calloc( (*tn) , double ) ; // likelihood of stencil parameter, t + I*2e 
	double *t_m1 = (double*) Calloc( (*tn) * (*tn) , double ) ; // stencil parameter, t - Ie 
	double *t_m1_lik = (double*) Calloc( (*tn) , double ) ;    
	double *t_m2 = (double*) Calloc( (*tn) * (*tn) , double ) ; // t - I*2e 
	double *t_m2_lik = (double*) Calloc( (*tn) , double ) ; 
	double *del = (double*) Calloc( (*tn) , double ) ; // optimization differences  
	
	int optimization_failures = 0 ; 
	int flag = 1 ; 
	int i, j, k , nan_sum , n_bad_del ; 
	for( i = 0 ; i < *max_it && flag > 0 ; i++ ) 
	{ 
		// Check for interrupts 
		R_CheckUserInterrupt() ;  
		
		// Construct stencils  
		for( j = 0 ; j < (*tn) ; j++ ) 
		{ 
			memcpy( t_p1 + j*(*tn) , t , (*tn) * sizeof(double) ) ; 
			memcpy( t_p2 + j*(*tn) , t , (*tn) * sizeof(double) ) ;	
			memcpy( t_m1 + j*(*tn) , t , (*tn) * sizeof(double) ) ; 
			memcpy( t_m2 + j*(*tn) , t , (*tn) * sizeof(double) ) ; 
			
			// edit values 
			if( t[j] == 0 ) 
			{ 
				t_p1[ j*(*tn) + j ] = *eps ; 
				t_p2[ j*(*tn) + j ] = *eps ; 
				t_m1[ j*(*tn) + j ] = *eps ; 
				t_m2[ j*(*tn) + j ] = *eps ; 
			}  
			else 
			{ 
				t_p1[ j*(*tn) + j ] *= (1+ (*eps)) ; 
				t_p2[ j*(*tn) + j ] *= (1+ (*eps)) ; 
				t_m1[ j*(*tn) + j ] *= (1+ (*eps)) ; 
				t_m2[ j*(*tn) + j ] *= (1+ (*eps)) ; 
			} 
		}  
		
		// Evaluate stencils 
		if( i == 0 || isnan(t0_lik) || isinf(t0_lik) )   
			log_lik_batch( t , 1 , &a , 1 , &t0_lik ) ; 
		log_lik_batch( t_p1 , (*tn) , &a , *batch_size , t_p1_lik ) ; 
		log_lik_batch( t_p2 , (*tn) , &a , *batch_size , t_p2_lik ) ; 
		log_lik_batch( t_m1 , (*tn) , &a , *batch_size , t_m1_lik ) ; 
		log_lik_batch( t_m2 , (*tn) , &a , *batch_size , t_m2_lik ) ; 
		
		// Calculate differences 
		for( j = 0 ; j < (*tn) ; j++ ) 
		{ 
			del[j] = -1.0*t_p2_lik[j] + 8.0*t_p1_lik[j] - 8.0*t_m1_lik[j] - 1.0*t_m2_lik[j] ;  
			del[j] /= -1.0*t_p2_lik[j] + 16.0*t_p1_lik[j] - 30.0*t0_lik + 16.0*t_m1_lik[j] - 1.0*t_m2_lik[j] ; 
			del[j] *= (*eps) ; 
			if( isnan(del[j]) || isinf(del[j]) ) 
				del[j] = 0.0 ; 
		} 
		
		// count failed differences   
		n_bad_del = 0 ; 
		for( j = 0 ; j < (*tn) ; j++ ) 
		{ 
			if( del[j] == 0.0 ) 
				n_bad_del++ ; 
		} 
		
		// if differences succeeded, test a line of points  
		if( n_bad_del < (*tn) )  
		{ 
			for( j = 0 ; j < (*test_coefs_n) ; j++ ) 
			{ 
				memcpy( t_test + j*(*tn) , t , (*tn) * sizeof(double) ) ; 
				for( k = 0 ; k < (*tn) ; k++ ) 
					t_test[ j*(*tn) + k] -= test_coefs[j] * del[k] ; // Scaled Newton's method iterate 
			} 
			log_lik_batch( t_test , (*test_coefs_n) , &a , *batch_size , t_test_lik ) ; 
		} 
		
		// Check for optimization  
		optimization_failures++ ; 
		nan_sum = 0 ; 
		for( j = 0 ; j < (*tn) ; j++ ) 
		{ 
			// p1  
			if( isnan(t_p1_lik[j]) || isinf(t_p1_lik[j]) ) 
				nan_sum++ ; 
			else // check for increase 
			{ 
				if( t_p1_lik[j] > t0_lik || real_check(t_p1_lik[j],t0_lik) ) 
				{ 
					optimization_failures = 0 ; 
					t0_lik = t_p1_lik[j] ; 
					memcpy( t , t_p1 + j*(*tn) , (*tn) * sizeof(double) ) ; 
				}  
			}  
			
			// p2 
			if( isnan(t_p2_lik[j]) || isinf(t_p1_lik[j]) ) 
				nan_sum++ ; 
			else 
			{ 
				if( t_p2_lik[j] > t0_lik || real_check(t_p2_lik[j],t0_lik) ) 
				{ 
					optimization_failures = 0 ; 
					t0_lik = t_p2_lik[j] ; 
					memcpy( t , t_p2 + j*(*tn) , (*tn) * sizeof(double) ) ; 
				} 
			} 
			
			// m1 
			if( isnan(t_m1_lik[j]) || isinf(t_m1_lik[j]) )  
				nan_sum++ ; 
			else 
			{ 
				if( t_m1_lik[j] > t0_lik || real_check(t_m1_lik[j],t0_lik) ) 
				{ 
					optimization_failures = 0 ; 
					t0_lik = t_m1_lik[j] ; 
					memcpy( t , t_m1 + j*(*tn) , (*tn) * sizeof(double) ) ; 
				} 
			} 
			
			// m2  
			if( isnan(t_m2_lik[j]) || isinf(t_m2_lik[j]) ) 
				nan_sum++ ; 
			else 
			{ 
				if( t_m2_lik[j] > t0_lik || real_check(t_m1_lik[j],t0_lik) ) 
				{ 
					optimization_failures = 0 ; 
					t0_lik = t_m2_lik[j] ; 
					memcpy( t , t_m2 + j*(*tn) , (*tn) * sizeof(double) ) ; 
				} 
			} 
		} 
		
		// line search test points  
		if( n_bad_del < (*tn) ) 
		{ 
			for( j = 0 ; j < (*test_coefs_n) ; j++ ) 
			{ 
				if( isnan(t_test_lik[j]) || isinf(t_test_lik[j]) ) 
					nan_sum++ ; 
				else 
				{ 
					if( t_test_lik[j] > t0_lik || real_check(t_test_lik[j],t0_lik) ) 
					{ 
						optimization_failures = 0 ; 
						t0_lik = t_test_lik[j] ; 
						memcpy( t , t_test + j*(*tn) , (*tn) * sizeof(double) ) ; 
					} 
				} 
			} 
		} 
		else 
			nan_sum += (*test_coefs_n) ; 
		
		if( optimization_failures > *min_it ) 
			flag = 0 ; // stop iterating 
		
		fprintf( stdout , "log lik: %e, nan_sum: %i\n" , t0_lik , nan_sum ) ;  
	} 
	*max_it = i ; // output iterations  
	*out = t0_lik ; 
	
	Free( t_test ) ; 
	Free( t_test_lik ) ; 
	Free( t_p1 ) ; 
	Free( t_p1_lik ) ; 
	Free( t_m1 ) ; 
	Free( t_m1_lik ) ; 
	Free( t_p2 ) ; 
	Free( t_p2_lik ) ; 
	Free( t_m2 ) ; 
	Free( t_m2_lik ) ; 
	Free( del ) ; 
} 





















