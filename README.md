# Musethereal 

## Overview

This repository contains code for the [MakeFashion](http://www.makefashion.ca/) project known as Musethereal.
Musethereal is a dress modelled after the [neuron](https://en.wikipedia.org/wiki/Neuron).  This repository contains the code for the conceptual nucleus and axon.  

![alt text](https://raw.githubusercontent.com/GrooveTherapy/musethereal/master/musetherealfull.jpg "Musethereal")

## Technology Stack

Musethereal uses:

1. The [Emotiv EPOC+ EEG Headset](https://emotiv.com/epoc.php) to pull activity from the brain

2. An Android phone (we used the LG Nexus 5) connected to the EPOC+ via bluetooth and using the Emotiv [community sdk](https://github.com/Emotiv/community-sdk) to pull data from the headset

3. [felHR85's UsbSerial](https://github.com/felHR85/UsbSerial) library was used to communicate data from the EPOC+ to the Arduino

4. The Arduino uses [Adafruit Neopixel RGB LEDs](https://www.adafruit.com/products/1655) to represent brain state via color
