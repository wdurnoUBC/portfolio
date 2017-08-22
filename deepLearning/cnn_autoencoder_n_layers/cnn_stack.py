
import tensorflow as tf 
import numpy as np 
import warnings 

# x must have a completely defined shape  
def stack_cnns ( x , filters=[ [3,3,3,32] , [5,5,32,15] ] , strides=[[1,1,1,1], [1,3,3,1]] , namescope="cnn_stack" , eps=1. ) : 
	'''
	Stacks convlutional neural nets in an autoencoder.  
	Each layer gets its own measurement of error. 
	ASSUMES:  
	x must have a completely defined shape. 
	RETURNS: 
	A 3-tuple 
	( CNN output , err list per CNN layer, parameters , deconvolution outputs ) 
	DOES NOT RETURN: 
	Parameters for deconvolutions.  
	''' 
	# Check x has a completely defined shape 
	if np.sum( [ str(x.shape[i]) == '?' for i in range(len(x.shape)) ] ) > 0 : 
		warnings.warn( 'stack_cnns: x must have completely defined shape. Returning None.' ) 
		return None 
	data = x 
	with tf.name_scope( namescope ) : 
		errs = [] 
		params = { 'cnn_w' : [] , 'cnn_b' : [] , 'inv_w' : [] , 'inv_b' : [] } 
		input_shapes = []  
		deconvolutions = [] 
		for i in range( len(filters) ) : 
			layer_name = namescope + "_" + str(i) 
			with tf.name_scope( layer_name ) : 
				# Store input shape for inversion 
				input_shapes.append( [ int(x.shape[j]) for j in range(len(x.shape)) ] ) 
				# Initialize parameters for this layer 
				params['cnn_w'].append( tf.Variable( tf.random_uniform( filters[i] , minval=-eps , maxval=eps ) , name = layer_name + '_cnn_var_w_' + str(i) ) ) 
				params['cnn_b'].append( tf.Variable( tf.random_uniform( [ filters[i][ -1 ] ] , minval=eps , maxval=-eps ) , name = layer_name + '_cnn_var_b_' + str(i) ) ) 
				# Define a CNN layer  
				x = tf.nn.conv2d( x , params['cnn_w'][i] , strides=strides[i] , padding='SAME' , name = layer_name + '_cnn_w_' + str(i) ) 
				x = tf.nn.bias_add( x , params['cnn_b'][i] , name = layer_name + '_cnn_b_' + str(i) ) 
				x = tf.nn.relu( x , name = layer_name + "_cnn_" + str(i) ) 
				# Invert all CNN layers up to this point 
				errs.append( x ) 
				params['inv_w'].append([]) 
				params['inv_b'].append([]) 
				for j in list( reversed( range(i+1) ) ) : 
					# Initialize parameters  
					cnn_inv_w = tf.Variable( tf.random_uniform( filters[j] , minval=-eps , maxval=eps ) , name = layer_name + '_cnn_inv_var_w_' + str(i) + '_' + str(j) ) 
					cnn_inv_b = tf.Variable( tf.random_uniform( [ input_shapes[j][ -1 ] ] , minval=-eps , maxval=eps ) , name = layer_name + '_cnn_inv_b_var_' + str(i) + '_' + str(j) ) 
					params['inv_w'][i].append( cnn_inv_w ) 
					params['inv_b'][i].append( cnn_inv_b ) 
					errs[i] = tf.nn.conv2d_transpose( errs[i] , cnn_inv_w , input_shapes[j] , strides[j] , padding='SAME' , name = layer_name + '_cnn_inv_b_' + str(i) + '_' + str(j) ) 
					errs[i] = tf.nn.bias_add( errs[i] , cnn_inv_b , name = layer_name + '_cnn_inv_b_' + str(i) + '_' + str(j) ) 
					if j > 0 : 
						# Use ReLU if not the final layer 
						errs[i] = tf.nn.relu( errs[i] , name = layer_name + "_cnn_inv_" + str(i) + "_" + str(j) ) 
					else : 
						# calculate final error  
						errs[i] = tf.nn.sigmoid( errs[i] , name = layer_name + "_cnn_inv_" + str(i) + "_" + str(j) ) 
						deconvolutions.append( errs[i] ) 
						errs[i] = tf.reduce_sum( tf.square( tf.subtract( errs[i] , data ) ) , name = layer_name + "_err_" + str(i) ) 
		return ( x , errs , params , deconvolutions ) 





