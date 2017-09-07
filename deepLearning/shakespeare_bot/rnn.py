
# Predict characters, one at a time, with a conv net.   

from __future__ import print_function

import tensorflow as tf
import fast_shakes_3 as fs # fast-access Shakespeare 
import numpy as np 
import time , sys , os 

# Parameters  
in_len = 200 
out_len = 1 
learning_rate = 0.001
batch_size = 500
display_step = 10 
rnn_layers = 2   
n_hidden = 200  
n_classes = 256 
embedding_dims = 8 # = log_2(256), if characters are truly independent, they can still have their own quadrants.  

f = fs.FastShakes( in_len , out_len ) 

# tf Graph input
x_tf = tf.placeholder( tf.float32 , [None, in_len, n_classes] ) 
y_tf = tf.placeholder( tf.float32 , [None, out_len , n_classes]) 

#### 
# Model design 
# 
# 1-Character prediction  
# DENSE   
# RNN  
# RNN 
# CNN 
# CNN  
# CNN 
# CNN 
# String  

# cnn 
with tf.name_scope( 'cnn' ) : 
	wc1 = tf.Variable( tf.random_normal( [2,n_classes,32] , stddev=1./np.sqrt(2) ) ) 
	wc2 = tf.Variable( tf.random_normal( [4,32,64] , stddev=1./np.sqrt(4*32) ) ) 
	wc3 = tf.Variable( tf.random_normal( [8,64,128] , stddev=1./np.sqrt(8*64) ) ) 
	wc4 = tf.Variable( tf.random_normal( [16,128,256] , stddev=1./np.sqrt(16*128) ) ) 
	wc5 = tf.Variable( tf.random_normal( [32,256,256] , stddev=1./np.sqrt(32*128) ) ) 
	wc6 = tf.Variable( tf.random_normal( [64,256,256] , stddev=1./np.sqrt(32*128) ) ) 
	bc1 = tf.Variable( tf.zeros( [32] ) ) 
	bc2 = tf.Variable( tf.zeros( [64] ) ) 
	bc3 = tf.Variable( tf.zeros( [128] ) )  
	bc4 = tf.Variable( tf.zeros( [256] ) ) 
	bc5 = tf.Variable( tf.zeros( [256] ) ) 
	bc6 = tf.Variable( tf.zeros( [256] ) ) 
	conv1 = tf.nn.conv1d( x_tf , wc1 , stride=1 , padding='SAME' ) 
	conv1 = tf.nn.bias_add( conv1 , bc1 ) 
	conv1 = tf.nn.relu( conv1 ) 
	conv2 = tf.nn.conv1d( conv1 , wc2 , stride=1 , padding='SAME' ) 
	conv2 = tf.nn.bias_add( conv2 , bc2 ) 
	conv2 = tf.nn.relu( conv2 ) 
	conv3 = tf.nn.conv1d( conv2 , wc3 , stride=2 , padding='SAME' ) 
	conv3 = tf.nn.bias_add( conv3 , bc3 ) 
	conv3 = tf.nn.relu( conv3 ) 
	conv4 = tf.nn.conv1d( conv3 , wc4 , stride=3 , padding='SAME' ) 
	conv4 = tf.nn.bias_add( conv4 , bc4 ) 
	conv4 = tf.nn.relu( conv4 ) 
	conv5 = tf.nn.conv1d( conv4 , wc5 , stride=2 , padding='SAME' ) 
	conv5 = tf.nn.bias_add( conv5 , bc5 ) 
	conv5 = tf.nn.relu( conv5 ) 
	conv6 = tf.nn.conv1d( conv5 , wc6 , stride=3 , padding='SAME' ) 
	conv6 = tf.nn.bias_add( conv6 , bc6 ) 
	conv6 = tf.nn.relu( conv6 ) 

# rnn 
with tf.name_scope( 'rnn' ) : 
	cell_layers = [ tf.contrib.rnn.GRUCell( n_hidden , kernel_initializer = tf.contrib.layers.xavier_initializer() , bias_initializer = tf.zeros_initializer() ) for layer in range(rnn_layers) ]
	multi_layer_cell = tf.contrib.rnn.MultiRNNCell( cell_layers ) 
	rnn_out, _ = tf.nn.dynamic_rnn( multi_layer_cell , conv4 , dtype=tf.float32 ) 

# dense 
with tf.name_scope( 'dense' ) : 
	wd1 = tf.Variable( tf.random_normal( [ n_hidden , n_classes ] , stddev=np.sqrt( 2./( n_hidden + n_classes ) ) ) )
	bd1 = tf.Variable( tf.zeros([ n_classes ]) ) 
	dense1 = tf.unstack( rnn_out , axis=1 )[ in_len/3/2 ] # last entry in rnn output 
	dense1 = tf.matmul( dense1 , wd1 ) 
	logits_tf = tf.nn.bias_add( dense1 , bd1 ) # output logits  

#### 
# optimization 

with tf.name_scope( 'metrics' ) : 
	loss_tf = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits( logits = logits_tf , labels = y_tf ) , name='loss_tf' ) 
	pred_tf = tf.argmax( logits_tf , 1 , name='pred_tf' ) 
	acc_tf = tf.reduce_mean( tf.cast( tf.equal( pred_tf , tf.argmax( tf.reshape( y_tf , [-1,n_classes] ) , 1)) , tf.float32 ) , name='acc_tf' ) 

with tf.name_scope( 'optimization' ) : 
	optimizer = tf.train.AdamOptimizer( learning_rate = learning_rate ).minimize( loss_tf ) 

####  
# Initialize 

init = tf.global_variables_initializer() 
saver = tf.train.Saver() 
sess = tf.Session() 
sess.run( init ) 

#### 
# tensorboard  

tensorboard_dir = '/tmp/tensorboard/item_' + str( time.time() ) + '/'
print( tensorboard_dir )
writer = tf.summary.FileWriter( tensorboard_dir )
writer.add_graph( sess.graph )
_ = tf.summary.scalar( 'loss' , loss_tf ) 
_ = tf.summary.scalar( 'acc' , acc_tf ) 
smy = tf.summary.merge_all() 

#### 
# training 

# data pipe 
#from multiprocessing.pool import ThreadPool
#nc = 16 # nc cores 
#pool = ThreadPool(nc)
#pool_out = [ pool.apply_async( f.next_batch , (batch_size, i,) ) for i in range(nc) ] # async data loading  

def train() : 
	j = 0 
	while 1 > 0 : 
		x , y = f.next_batch( batch_size ) 
		# x , y = pool_out[j].get() # get data (blocking) 
		# pool_out[j] = pool.apply_async( f.next_batch , (batch_size, train.step+nc,) ) # Get more data (non-blocking) 
		# j += 1 
		# if j >= nc : 
		#	j = 0 
		_ , loss , acc , s = sess.run( [optimizer, loss_tf, acc_tf, smy] , feed_dict = {x_tf : x, y_tf : y}) 
		writer.add_summary( s , train.step ) 
		print("\rIter " + str(train.step) + ", loss= " +"{:.6f}".format(loss) + ", acc= " +"{:.6f}".format(acc) , end="") 
		sys.stdout.flush() 
		train.step += 1 

train.step = 0 

####  
# prediction  

def pred_char ( x=None ) :  
	_ , y = f.next() # super lazy here   
	y = np.reshape( y , [1,1,n_classes] ) 
	if x is None : 
		x , _ = f.next() 
	x = np.reshape( x , [1,in_len,n_classes] ) 
	pred = sess.run( pred_tf , feed_dict = {x_tf : x, y_tf : y} ) 
	pred = fs.to_ascii[ int( pred ) ] 
	x = np.reshape( x , [in_len,n_classes] ) 
	x = [ np.argmax( x[i,] ) for i in range(len(x)) ] 
	x = fs.vec_to_str( x ) 
	return x + pred 

def pred( n = in_len ) : 
	x= pred_char() 
	x0 = '' 
	for i in range(n) : 
		x = [ fs.ascii_val_to_one_hot( fs.from_ascii[ x[i] ] ) for i in range(len(x)) ] 
		x = np.array( x ) 
		x = x[1:,] 
		x = pred_char( x ) 
		x0 = x0 + x[ len(x)-1 ] 
	return x0  

####
# saving and loading 

def save_model ( path='models/model.ckpt' ) : 
	 return saver.save( sess , path ) 

def load_model ( path='models/model.ckpt' ) : 
	saver.restore( sess , path ) 














