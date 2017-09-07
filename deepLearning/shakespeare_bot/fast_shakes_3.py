
import random
import numpy as np 

from_ascii = dict()
for i in range(256) :
    from_ascii[ chr(i) ] = i 

to_ascii = [ chr(i) for i in range(256) ]  

# load all of Shakespeare's works as one string. 
sh = "".join( open("data/alllines.txt").readlines() )
sh_n = len(sh)

def get_sh_char ( i ) : 
	return sh[ i % sh_n ]

def vec_to_str ( v ) : 
	v = [ to_ascii[ int(i) ] for i in v ] 
	return "".join(v) 

def ascii_val_to_one_hot ( ascii_val=0 ) : 
	out = np.zeros( 256 ) 
	out[ ascii_val ] = 1 
	return out 

def one_hot_to_ascii_val ( v ) : 
	return int( np.dot( range(256) , v ) ) 

class FastShakes : 
	
	def __init__ ( self , in_len=5 , out_len=20 ) : 
		if in_len < 2 : 
			in_len = 2 
		if out_len < 1 : 
			out_len = 1 
		self.in_len = in_len  
		self.out_len = out_len 
		sh_i = random.randrange( sh_n ) 
		self.x = np.array( [ ascii_val_to_one_hot( from_ascii[ get_sh_char(i) ] ) for i in range( sh_i , sh_i + in_len ) ] ) 
		self.y = np.array( [ ascii_val_to_one_hot( from_ascii[ get_sh_char(i) ] ) for i in range( sh_i + in_len , sh_i + in_len + out_len ) ] ) 
	
	def next ( self ) : 
		sh_i = random.randrange( sh_n ) 
		self.x = np.array( [ ascii_val_to_one_hot( from_ascii[ get_sh_char(i) ] ) for i in range( sh_i , sh_i + self.in_len ) ] ) 
		self.y = np.array( [ ascii_val_to_one_hot( from_ascii[ get_sh_char(i) ] ) for i in range( sh_i + self.in_len , sh_i + self.in_len + self.out_len ) ] ) 
		return self.x , self.y 

	def next_batch( self , n=3 , seed = None ) : 
		if not seed == None : 
			random.seed( seed ) 
		x_out = np.zeros( [ n , self.in_len , 256 ] ) 
		y_out = np.zeros( [ n , self.out_len , 256 ] ) 
		for i in range(n) : 
			tmp = self.next() 
			x_out[i,] = tmp[0] 
			y_out[i,] = tmp[1] 
		return x_out , y_out 



