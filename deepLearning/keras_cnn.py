''' This script is a modified version 
of one available from blog.keras.io. 
''' 

import os 
from keras.preprocessing.image import ImageDataGenerator
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D
from keras.layers import Activation, Dropout, Flatten, Dense
from keras import backend as K


# dimensions of our images.
img_width, img_height = 150, 150

train_data_dir = 'data/train'
validation_data_dir = 'data/validation'
nb_train_samples = len( os.listdir('data/train/cats') ) + len( os.listdir('data/train/dogs') )
nb_validation_samples = len( os.listdir('data/validation/cats') ) + len( os.listdir('data/validation/dogs') )
epochs = 10 
batch_size = 32

if K.image_data_format() == 'channels_first':
    input_shape = (3, img_width, img_height)
else:
    input_shape = (img_width, img_height, 3)

model = Sequential()
model.add(Conv2D(32, (3, 3), input_shape=input_shape))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))

model.add(Conv2D(32, (3, 3)))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))

model.add(Conv2D(64, (3, 3)))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))

model.add(Conv2D(64, (3, 3)))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2))) 

model.add(Conv2D(64, (3, 3)))
model.add(Activation('relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))

model.add(Flatten())
model.add(Dense(64))
model.add(Activation('relu'))
model.add(Dropout(0.5))
model.add(Dense(1))
model.add(Activation('sigmoid'))

model.compile(loss='binary_crossentropy',
              optimizer='rmsprop',
              metrics=['accuracy'])

# this is the augmentation configuration we will use for training
train_datagen = ImageDataGenerator(
    rescale=1. / 255,
    shear_range=0.2,
    zoom_range=0.2,
    horizontal_flip=True)

# this is the augmentation configuration we will use for testing:
# only rescaling
test_datagen = ImageDataGenerator(rescale=1. / 255)

train_generator = train_datagen.flow_from_directory(
    train_data_dir,
    target_size=(img_width, img_height),
    batch_size=batch_size,
    class_mode='binary')

validation_generator = test_datagen.flow_from_directory(
    validation_data_dir,
    target_size=(img_width, img_height),
    batch_size=batch_size,
    class_mode='binary')

# Check if model is already trained, 
# if so load it 
# if not, train it 
if os.path.isfile( 'second_try.h6' ) : # file exists 
    model.load_weights( 'second_try.h6' ) 
else :  
    model.fit_generator(
        train_generator,
        steps_per_epoch=nb_train_samples // batch_size,
        epochs=epochs,
        validation_data=validation_generator,
        validation_steps=nb_validation_samples // batch_size)
    model.save_weights('second_try.h6') 

# Demonstrate   
from scipy.misc import imsave, imread, imresize 
import numpy as np 

# This just returns 1s...  
def predict ( img_path='test1/1.jpg' ) :  
    x = imread( img_path , mode='RGB' ) 
    x = imresize( x , ( img_width , img_height ) ) 
    x = x.reshape( 1 , img_width , img_height , 3 ) 
    x = x * (1. / 255) # rescale 
    val = model.predict(x) 
    return( np.array_str(val) ) 

def predicti ( i = 1 ) :  
    f_str = 'test1/' + str(i) + '.jpg' 
    print(f_str) 
    return( predict( f_str ) ) 




