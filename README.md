#Musethereal

###[See it in action!](https://www.youtube.com/watch?v=KzFbf7iRgXo)
###[See it on CBC!](http://www.cbc.ca/beta/news/canada/calgary/calgary-fashion-technology-3d-1.3515294)

##Overview

This repository contains code for the [MakeFashion](http://www.makefashion.ca/) project known as Musethereal. 
Musethereal is a dress modelled after the [neuron](https://en.wikipedia.org/wiki/Neuron). This repository contains the code for the conceptual dendrites, responding to the position of the accelerometer tilt.

![alt text](https://raw.githubusercontent.com/GrooveTherapy/musethereal/master/musetherealfull.jpg "Musethereal")

##Technology Stack
Musethereal uses:

1. The [Emotiv EPOC+ EEG Headset](https://emotiv.com/epoc.php) to pull activity from the brain

2. An Android phone (we used the LG Nexus 5) connected to the EPOC+ via bluetooth and using the Emotiv [community sdk](https://github.com/Emotiv/community-sdk) to pull data from the headset

3. [felHR85's UsbSerial](https://github.com/felHR85/UsbSerial) library was used to communicate data from the EPOC+ to the Arduino

4. The Arduino [Trinket Pro 5V](https://www.adafruit.com/product/2000) uses [Adafruit Neopixel RGB LEDs](https://www.adafruit.com/products/1655) to represent the chasing, cascading sequences, corresponding to the accelerometer tilt on the arms and on the skirt of the dress as appliqués.

5. [Adafruit Neopixel](https://github.com/adafruit/Adafruit_NeoPixel) library was used to set RGB LEDs' sequences

6. [ADXL335 - 5V ready triple-axis accelerometer](https://www.adafruit.com/products/163) was used to read the analog value of the x, y and z direction of the hand gestures and output it to the LED strip as a chase sequence
