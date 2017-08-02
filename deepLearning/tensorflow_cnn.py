
from __future__ import print_function 

########################## Get image paths 

from os import listdir 

#train_path = '../data/train/' 
train_path = 'extra_data/' # pre-processing applied  
valid_path = '../data/validation/' 
test_path  = '../test1/' 
train_path = 'extra_data/'

train_cats = listdir( train_path + 'cats/' ) 
train_dogs = listdir( train_path + 'dogs/' ) 
valid_cats = listdir( valid_path + 'cats/' ) 
valid_dogs = listdir( valid_path + 'dogs/' ) 
test_files = listdir( test_path ) 

train_cat_paths  = [ train_path + 'cats/' + s for s in train_cats ] 
train_dog_paths  = [ train_path + 'dogs/' + s for s in train_dogs ] 
valid_cat_paths  = [ valid_path + 'cats/' + s for s in valid_cats ] 
valid_dog_paths  = [ valid_path + 'dogs/' + s for s in valid_dogs ] 
test_paths = [ test_path + s for s in train_cats ] 

########################## Image handling functions 

from scipy.misc import imread, imresize 
import numpy as np 
import random 
import tensorflow as tf 

# parameters 
img_height = 150 
img_width  = 150 

# returns np.ndarray of RGB image  
def load_image ( path ) : 
	img = imread( path , mode='RGB' ) 
	# img = imresize( img , ( img_width , img_height ) ) 
	img = img * (1. / 255) # rescale 
	return( img )  

# returns np.ndarray of RGB images  
def load_image_batch ( path_list ) : 
	n = len( path_list ) 
	out = np.ndarray( (n , img_width , img_height , 3) ) 
	for i in range(0,n) : 
		out[i,] = load_image( path_list[i] ) 
	return( out ) 

def load_random_image_batch ( path_list , batch_size ) : 
	n = len( path_list ) 
	idx = random.sample( range(0,n) , batch_size ) 
	sub_path_list = [ path_list[i] for i in idx ] 
	return( load_image_batch( sub_path_list ) ) 

def load_training_batch ( batch_size , cat_paths = train_cat_paths , dog_paths = train_dog_paths ) : 
	n_cats = np.random.binomial( batch_size , 0.5 )  
	n_dogs = batch_size - n_cats 
	cats = load_random_image_batch( cat_paths , n_cats ) 
	dogs = load_random_image_batch( dog_paths , n_dogs ) 
	cat_y = np.zeros( [ n_cats , 2 ] ) 
	cat_y[:,0] = 1 
	dog_y = np.zeros( [ n_dogs , 2 ] ) 
	dog_y[:,1] = 1 
	x = np.vstack( ( cats , dogs ) ) 
	y = np.vstack( ( cat_y , dog_y ) ) 
	idx = random.sample( range(batch_size) , batch_size ) 
	x = np.array( [ x[i,] for i in idx ] ) 
	y = np.array( [ y[i,] for i in idx ] ) 
	return( x , y ) 
	

########################## Define CNN model  

# parameters
learning_rate = 0.001 
training_iters = 10*11500*2  
batch_size = 160   
dropout = 0.5 

# tf Graph input 
x = tf.placeholder(tf.float32, [None, img_width , img_height , 3 ]) 
y = tf.placeholder(tf.float32, [None, 2 ]) 
keep_prob = tf.placeholder(tf.float32) #dropout (keep probability) 

def conv2d(x, W, b, strides=1): 
    # Conv2D wrapper, with bias and relu activation 
    x = tf.nn.conv2d(x, W, strides=[1, strides, strides, 1], padding='SAME') 
    x = tf.nn.bias_add(x, b) 
    return tf.nn.relu(x) 
	#return tf.nn.softplus(x) 

def maxpool2d(x, k=2):
    # MaxPool2D wrapper
    return tf.nn.max_pool(x, ksize=[1, k, k, 1], strides=[1, k, k, 1], padding='SAME')

# Tensorflow bug: this function cannot be used, and execution must be rolled out. 
# The final tf.add returns a tf variable which becomes None after return. 
# I'd have to guess this is some sort of garbage collection bug, but it's really 
# up to the devs. I've checked out the source code. It's not a simple fix.  
# Create model 
def conv_net(x, weights, biases, dropout):
    # Convolution Layer 
    conv1 = conv2d(x, weights['wc1'], biases['bc1'])
    # Max Pooling (down-sampling) 
    conv1 = maxpool2d(conv1, k=2)
	
    # Convolution Layer
    conv2 = conv2d(conv1, weights['wc2'], biases['bc2'])
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2, k=2)
	
    # Fully connected layer
    # Reshape conv2 output to fit fully connected layer input
    fc1 = tf.reshape(conv2, [-1, weights['wd1'].get_shape().as_list()[0]])
    fc1 = tf.add(tf.matmul(fc1, weights['wd1']), biases['bd1'])
    fc1 = tf.nn.relu(fc1)
    # Apply Dropout
    fc1 = tf.nn.dropout(fc1, dropout)
	
    # Output, class prediction
    out = tf.add(tf.matmul(fc1, weights['out']), biases['out']) 
    return out 

########################## Initialize model  

# initialize random parameters 
theta = { 'weights' : None , 'biases' : None }  
theta[ 'weights' ] = {
    # 3x3 conv, 3 inputs, 32 outputs 
    'wc1': tf.Variable(tf.random_normal([3, 3, 3, 32])), 
	# 3x3 conv, 32 inputs, 32 outputs 
	'wc2': tf.Variable(tf.random_normal([3, 3, 32, 32])), 
    # 3x3 conv, 32 inputs, 64 outputs 
    'wc3': tf.Variable(tf.random_normal([3, 3, 32, 64])), 
	# 3x3 conv, 64 inputs, 64 outputs 
	'wc4': tf.Variable(tf.random_normal([3, 3, 64, 64])), 
    # fully connected, 10*10*64 inputs, 1024 outputs 
    'wd1': tf.Variable(tf.random_normal([ 10*10*64 , 1024 ])),  
    # 1024 inputs, 2 outputs (class prediction) 
    'out': tf.Variable(tf.random_normal([1024, 2]))
}

theta[ 'biases' ] = { 
    'bc1': tf.Variable(tf.random_normal([32])), 
	'bc2': tf.Variable(tf.random_normal([32])), 
    'bc3': tf.Variable(tf.random_normal([64])), 
	'bc4': tf.Variable(tf.random_normal([64])), 
    'bd1': tf.Variable(tf.random_normal([1024])), 
    'out': tf.Variable(tf.random_normal([2])) 
}  

#### Duct tape: I'm rolling out conv_net so that 'pred' is not None 

weights = theta['weights'] 
biases = theta['biases'] 
# keep_prob = dropout  
conv1 = conv2d(x, weights['wc1'], biases['bc1']) 
conv1 = maxpool2d(conv1, k=2) 
conv2 = conv2d(conv1, weights['wc2'], biases['bc2']) 
conv2 = maxpool2d(conv2, k=2) 
conv3 = conv2d(conv2, weights['wc3'], biases['bc3']) 
conv3 = maxpool2d(conv3, k=2) 
conv4 = conv2d(conv3, weights['wc4'], biases['bc4']) 
conv4 = maxpool2d(conv4, k=2) 
fc1 = tf.reshape(conv4, [-1, weights['wd1'].get_shape().as_list()[0]]) 
fc1 = tf.add(tf.matmul(fc1, weights['wd1']), biases['bd1']) 
fc1 = tf.nn.relu(fc1) 
#fc1 = tf.nn.softplus(fc1) 
fc1 = tf.nn.dropout(fc1, keep_prob) 
pred = tf.add(tf.matmul(fc1, weights['out']), biases['out']) 

#### 

# Construct model 
# pred = conv_net(x, theta['weights'], theta['biases'], keep_prob) # BUG IN TENSORFLOW : tf.add returns None as function output. Fix : roll out function call.  

# Define loss and optimizer 
cost = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits=pred, labels=y))  
optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(cost) 

# Evaluate model
correct_pred = tf.equal(tf.argmax(pred, 1), tf.argmax(y, 1))  
accuracy = tf.reduce_mean(tf.cast(correct_pred, tf.float32)) 

# Initializing the variables
init = tf.global_variables_initializer() 

# Make sure we can save our parameters  
saver = tf.train.Saver() 

########################## Train model 

# We load our data while training  
from multiprocessing.pool import ThreadPool 
nc = 16 # nc cores 
pool = ThreadPool(nc) 
pool_out = [ pool.apply_async( load_training_batch , (batch_size,) ) for i in range(nc) ] # async data loading  
j = 0 # core index  

# running (moving) average accuracy  
racc = 0 

sess = tf.Session() 
sess.run( init ) 
step = 1 
while step * batch_size < training_iters: 
	batch_x, batch_y = pool_out[j].get() # get data, blocking 
	pool_out[j] = pool.apply_async( load_training_batch , (batch_size,) ) # launch a replacement job   
	j += 1 
	if j >= nc : 
		j = 0 # update core index 
	# run optimizer  
	_ , loss , acc = sess.run( [optimizer , cost , accuracy] , feed_dict={x: batch_x, y: batch_y, keep_prob: dropout}) 
	racc = 0.95 * racc + 0.05 * acc 
	step += 1 
	print("Work: " + "{:.5f}".format( 100.*(step*batch_size-1) / training_iters ) + "%, loss= " + "{:.6f}".format(loss) + ", acc= " + "{:.5f}".format(acc) + " " + "{:.5f}".format(racc) ) 

print("Optimization Finished!") 
x_valid , y_valid = load_training_batch ( batch_size , valid_cat_paths , valid_dog_paths ) 
print("Testing Accuracy:", sess.run(accuracy, feed_dict={x: x_valid , y: y_valid , keep_prob: 1.})) 



