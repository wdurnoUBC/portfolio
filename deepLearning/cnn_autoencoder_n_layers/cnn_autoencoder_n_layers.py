
### LIBRARIES 

from __future__ import print_function 
from scipy.misc import imread, imresize 
import sys, os  
import numpy as np
import random
import tensorflow as tf 
import load_imgs as li # my own data loader  
import cnn_stack 
import my_plot as mp 
import time 

### PARAMETERS 

learning_rate = 0.01
#training_iters = 1000*11500*2
batch_size = 70 
img_width = img_height = 150 

### MODEL  

filters = [ [3,3,3,15] , 
			[3,3,15,32] , 
			[3,3,32,64] , 
			[3,3,64,64] ] 
strides = [ [1,1,1,1] , 
			[1,1,1,1] , 
			[1,1,1,1] , 
			[1,1,1,1] ] 

# images tensor  
x_tf = tf.placeholder(tf.float32, [batch_size, img_width , img_height , 3 ]) 
y_tf = tf.placeholder(tf.float32, [batch_size , 2] ) 

# Define the model  
last_conv , errs_tf , params , deconv_tf = cnn_stack.stack_cnns( x_tf , filters , strides , eps=0.001 ) 

### EVALUATION 

err_tf = tf.reduce_sum( errs_tf , name='err' ) 
optimizer = tf.train.RMSPropOptimizer( learning_rate = learning_rate ).minimize(err_tf)  

opts = [ tf.train.RMSPropOptimizer( learning_rate = learning_rate ).minimize( err_i ) for err_i in errs_tf ] 

### Image classification   

flat_dim = np.prod( [ int(i) for i in last_conv.shape[1:] ] ) 
last_conv_flat = tf.reshape( last_conv , [batch_size, flat_dim ] )  
dense_w = tf.Variable(tf.random_uniform([ flat_dim , 2], minval=-.00001,maxval=.00001), name='dense_w')
dense_b = tf.Variable(tf.random_uniform([2], minval=-.00001 , maxval=.00001), name='dense_b')
logits = tf.matmul( last_conv_flat , dense_w )
logits = tf.add( logits , dense_b , name='logits' )

classification_error = tf.reduce_sum(tf.nn.softmax_cross_entropy_with_logits( logits=logits , labels=y_tf )) 
classification_optimizer = tf.train.RMSPropOptimizer( learning_rate = learning_rate ).minimize( classification_error ) 

classifications = tf.nn.softmax( logits ) 
acc_tf = tf.reduce_mean( tf.square( tf.subtract( tf.argmax(classifications,1) , tf.argmax(y_tf,1) ) ) ) 
# classification_optimizer = tf.train.RMSPropOptimizer( learning_rate = learning_rate ).minimize( tf.negative( acc_tf ) )  

### TRAIN  

saver = tf.train.Saver() 
 
# Data pipe  

from multiprocessing.pool import ThreadPool
nc = 16 # nc cores 
pool = ThreadPool(nc)
pool_out = [ pool.apply_async( li.load_training_batch , (batch_size,i,) ) for i in range(nc) ] # async data loading  
  
init = tf.global_variables_initializer() 
sess = tf.Session() 
sess.run( init ) 

# TENSORBOARD 

tensorboard_dir = '/tmp/tensorboard/item_' + str( time.time() ) + '/' 
print( tensorboard_dir ) 
writer = tf.summary.FileWriter( tensorboard_dir ) 
writer.add_graph( sess.graph ) 
tf.summary.scalar( 'error' , err_tf ) 
for i in range(len(filters)) : 
	tf.summary.histogram( 'cnn_w_' + str(i) , params['cnn_w'][i] ) 
	tf.summary.histogram( 'cnn_b_' + str(i) , params['cnn_b'][i] ) 
summaries = tf.summary.merge_all() 

# train  
def train ( op=optimizer , max_it=float('inf') ) : 
	j = 0 
	step = 0 
	while max_it > step*batch_size : 
		x, y = pool_out[j].get() 
		pool_out[j] = pool.apply_async( li.load_training_batch , (batch_size,step+nc,) ) 
		j = j + 1 
		if j >= nc : 
			j = 0 
		step += 1 
		_ , er , s = sess.run( [ op , err_tf, summaries] , feed_dict={x_tf: x , y_tf: y } ) 
		writer.add_summary( s , step ) 
		if max_it < float('inf') : 
			print("\rWork: " + "{:.5f}".format( 100.*(step*batch_size-1)/max_it ) + "%, err= " + "{:.6f}".format(er) , end="" ) 
		else : 
			print("\rWork: " + "{:.0f}".format( step*batch_size ) + ", err= " + "{:.6f}".format(er) , end="" ) 
		sys.stdout.flush() 
	print("") # new line  

def train_by_layers () : 
	for i in range(len(opts)) : 
		print( 'layer ' + str(i) ) 
		train( opts[i] , 11500*2 ) 

def get_pred () : 
	x, _ = pool_out[0].get() 
	y = sess.run( deconv_tf , feed_dict={x_tf: x } ) 
	return x , y 

def train_classifier () : 
	j = 0 
	step = 0 
	while 1 > 0 : 
		x , y = pool_out[j].get() 
		pool_out[j] = pool.apply_async( li.load_training_batch , (batch_size,step+nc,) ) 
		j = j + 1 
		if j >= nc : 
			j = 0 
		_ , acc , s, er2 = sess.run( [ classification_optimizer , acc_tf, summaries, classification_error] , feed_dict={x_tf: x , y_tf: y } ) 
		print("\rWork: " + "{:.0f}".format( step*batch_size ) + ", acc= " + "{:.6f}".format(acc*100.) + ', xent: ' + "{:.6f}".format(er2) , end="" ) 
		sys.stdout.flush() 

def get_classification() : 
	x, y = pool_out[0].get() 
	z = sess.run( classifications , feed_dict={x_tf: x , y_tf : y } ) 
	return ( x , y , z ) 

def save_progress() :  
	saver.save(sess, "/tmp/model.ckpt") 

def load_model() : 
	saver.restore(sess, "/tmp/model.ckpt") 

def save_conv( save_dir = '/tmp/model/' , p=params ) : 
	if not os.path.exists( save_dir ) : 
		os.mkdir( save_dir ) 
	for i in range( len( filters ) ) : 
		w, b = sess.run( [ p['cnn_w'][i] , p['cnn_b'][i] ] ) 
		w.dump( save_dir + 'cnn_w_' + str(i) ) 
		b.dump( save_dir + 'cnn_b_' + str(i) ) 
		for j in range( i+1 ) : 
			w, b = sess.run( [ p['inv_w'][i][j] , p['inv_b'][i][j] ] ) 
			w.dump( save_dir + 'inv_w_' + str(i) + '_' + str(j) ) 
			b.dump( save_dir + 'inv_b_' + str(i) + '_' + str(j) ) 

def load_conv( save_dir = '/tmp/model/' , p=params ) : 
	for i in range( len( filters ) ) : 
		p['cnn_w'][i] = p['cnn_w'][i].assign( np.load( save_dir + 'cnn_w_' + str(i) ) ) 
		p['cnn_b'][i] = p['cnn_b'][i].assign( np.load( save_dir + 'cnn_b_' + str(i) ) ) 
		_ , _ = sess.run( [ p['cnn_w'][i] , p['cnn_b'][i] ] ) 
		for j in range( i+1 ) : 
			p['inv_w'][i][j] = p['inv_w'][i][j].assign( np.load( save_dir + 'inv_w_' + str(i) + '_' + str(j) ) ) 
			p['inv_b'][i][j] = p['inv_b'][i][j].assign( np.load( save_dir + 'inv_b_' + str(i) + '_' + str(j) ) ) 
			_ , _ = sess.run( [ p['inv_w'][i][j] , p['inv_b'][i][j] ] ) 











