
# This code runs a convolutional autoencoder with two layers  

### LIBRARIES 

from __future__ import print_function 
from scipy.misc import imread, imresize
import numpy as np
import random
import tensorflow as tf 
import load_imgs as li # my own data loader  

### PARAMETERS 

learning_rate = 0.01
training_iters = 1000*11500*2
batch_size = 160
dropout = 0.5 
img_width = img_height = 150 

### MODEL  

# images tensor  
x = tf.placeholder(tf.float32, [None, img_width , img_height , 3 ]) 

def conv2d(x, W, b, strides=1) : 
    # Conv2D wrapper, with bias and relu activation 
    x = tf.nn.conv2d(x, W, strides=[1, strides, strides, 1], padding='SAME') 
    x = tf.nn.bias_add(x, b) 
    return tf.nn.relu(x) 

def conv2d_inv( x , W , out_shape , b , strides=1 ) : 
	x = tf.nn.conv2d_transpose( x , W , out_shape , [1,strides,strides,1] , padding='SAME' ) 
	return tf.add( x , b ) 

def dense( x , W , b ) : 
	x = tf.matmul( x , W ) 
	x = tf.add( x , b ) 
	return tf.nn.sigmoid( x ) 

### ENCODER  

ew = { 
	'wc1' : tf.Variable(tf.random_uniform([3, 3, 3, 32],minval=-1.,maxval=1.)), # 3 X 3 convolution, 3 inputs, 32 outputs 
	'wc2' : tf.Variable(tf.random_uniform([5, 5, 32, 15],minval=-1.,maxval=1.))  
} 
eb = { 
	'bc1' : tf.Variable(tf.random_uniform([32],minval=-1.,maxval=1.)), 
	'bc2' : tf.Variable(tf.random_uniform([15],minval=-1.,maxval=1.)) 
} 

e_conv1 = conv2d( x , ew['wc1'] , eb['bc1'] , 1 ) 
e_conv2 = conv2d( e_conv1 , ew['wc2'] , eb['bc2'] , 3 ) 

### DECODER  

dw = { 
	'wc1' : tf.Variable(tf.random_uniform([5,5,32,15],minval=-1.,maxval=1.)), 
	'wc2' : tf.Variable(tf.random_uniform([3,3,3,32],minval=-1.,maxval=1.)) 
} 

db = { 
	'bc1' : tf.Variable(tf.random_uniform([32],minval=-1.,maxval=1.)), 
	'bc2' : tf.Variable(tf.random_uniform([3],minval=-1.,maxval=1.))  
} 

d_conv1 = conv2d_inv( e_conv2 , dw['wc1'] , [batch_size,150,150,32] , db['bc1'] , 3 ) 
d_conv1_relu = tf.nn.relu( d_conv1 ) 
d_conv2 = conv2d_inv( d_conv1_relu , dw['wc2'] , [batch_size,img_width,img_height,3] , db['bc2'] , 1 ) 

pred = tf.nn.sigmoid( d_conv2 ) 

### EVALUATION 

# Error will be calculated on inverse-sigmoid scale. 
eps = tf.constant( 0.00001 ) 
one = tf.constant( 1.0 ) 
def inv_sigmoid( x ) : 
	denom = tf.add( tf.subtract( one , x ) , eps ) # eps is for numerical stability  
	return tf.div( x , denom ) 

inv_sig_x = inv_sigmoid( x ) 

# err = tf.reduce_sum( tf.abs( tf.subtract( d_conv2 , inv_sig_x ) ) , name='err' ) 
err = tf.reduce_sum( tf.abs( tf.subtract( pred , x ) ) , name='err' ) 

optimizer = tf.train.RMSPropOptimizer( learning_rate = learning_rate ).minimize(err)  

### TRAIN  
 
# Data pipe  

from multiprocessing.pool import ThreadPool
nc = 16 # nc cores 
pool = ThreadPool(nc)
pool_out = [ pool.apply_async( li.load_training_batch , (batch_size,i,) ) for i in range(nc) ] # async data loading  
j = 0 # core index

# Run  
init = tf.global_variables_initializer() 

sess = tf.Session() 
sess.run( init ) 

step = 0 
while step*batch_size < training_iters : 
	batch_x, _ = pool_out[j].get() 
	pool_out[j] = pool.apply_async( li.load_training_batch , (batch_size,step+nc,) ) 
	j = j + 1 
	if j >= nc : 
		j = 0 
	step += 1 
	# batch_x, _ = li.load_training_batch( batch_size ) 
	_ , er = sess.run( [optimizer,err] , feed_dict={x: batch_x } ) 
	print("Work: " + "{:.5f}".format( 100.*(step*batch_size-1) / training_iters ) + "%, err= " + "{:.6f}".format(er) ) 



















