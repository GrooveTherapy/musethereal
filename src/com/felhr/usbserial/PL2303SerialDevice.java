package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class PL2303SerialDevice extends UsbSerialDevice
{
	private static final String CLASS_ID = PL2303SerialDevice.class.getSimpleName();
	
	private static final int PL2303_REQTYPE_HOST2DEVICE_VENDOR = 0x40;
	private static final int PL2303_REQTYPE_DEVICE2HOST_VENDOR = 0xC0;
	private static final int PL2303_REQTYPE_HOST2DEVICE = 0x21;
	
	private static final int PL2303_VENDOR_WRITE_REQUEST = 0x01;
	private static final int PL2303_SET_LINE_CODING = 0x20;
	private static final int PL2303_SET_CONTROL_REQUEST = 0x22;
	
	private byte[] defaultSetLine = new byte[]{
			(byte) 0x80, // [0:3] Baud rate (reverse hex encoding 9600:00 00 25 80 -> 80 25 00 00)
			(byte) 0x25,
			(byte) 0x00,
			(byte) 0x00,
			(byte) 0x00, // [4] Stop Bits (0=1, 1=1.5, 2=2)
			(byte) 0x00, // [5] Parity (0=NONE 1=ODD 2=EVEN 3=MARK 4=SPACE)
			(byte) 0x08  // [6] Data Bits (5=5, 6=6, 7=7, 8=8)
	};
	
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	private UsbRequest requestIN;
	
	
	public PL2303SerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
	}

	@Override
	public void open() 
	{
		// Restart the working thread and writeThread if it has been killed before and claim interface
		restartWorkingThread();
		restartWriteThread();
		mInterface = device.getInterface(0); // PL2303 devices have only one interface

		if(connection.claimInterface(mInterface, true))
		{
			Log.i(CLASS_ID, "Interface succesfully claimed");
		}else
		{
			Log.i(CLASS_ID, "Interface could not be claimed");
		}
		
		// Assign endpoints
		int numberEndpoints = mInterface.getEndpointCount();
		for(int i=0;i<=numberEndpoints-1;i++)
		{
			UsbEndpoint endpoint = mInterface.getEndpoint(i);
			if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
					&& endpoint.getDirection() == UsbConstants.USB_DIR_IN)
				inEndpoint = endpoint;
			else if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
					&& endpoint.getDirection() == UsbConstants.USB_DIR_OUT)
				outEndpoint = endpoint;	
		}
		
		//Default Setup
		byte[] buf = new byte[1];
			//Specific vendor stuff that I barely understand but It is on linux drivers, So I trust :)
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 0, null);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 1, null);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0000, 1, null);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0001, 0, null);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0002, 0x0044, null);
			// End of specific vendor stuff
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_CONTROL_REQUEST, 0x0003, 0,null);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0505, 0x1311, null);
		
		// Initialize UsbRequest
		requestIN = new UsbRequest();
		requestIN.initialize(connection, inEndpoint);

		// Pass references to the threads
		workerThread.setUsbRequest(requestIN);
		writeThread.setUsbEndpoint(outEndpoint);
	}

	@Override
	public void close() 
	{
		killWorkingThread();
		killWriteThread();
		connection.close();
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		byte[] tempBuffer = new byte[4];
		tempBuffer[0] = (byte) (baudRate & 0xff);
		tempBuffer[1] = (byte) (baudRate >> 8 & 0xff);
		tempBuffer[2] = (byte) (baudRate >> 16 & 0xff);
		tempBuffer[3] = (byte) (baudRate >> 24 & 0xff);
		if(tempBuffer[0] != defaultSetLine[0] || tempBuffer[1] != defaultSetLine[1] || tempBuffer[2] != defaultSetLine[2]
				|| tempBuffer[3] != defaultSetLine[3])
		{
			defaultSetLine[0] = tempBuffer[0]; 
			defaultSetLine[1] = tempBuffer[1]; 
			defaultSetLine[2] = tempBuffer[2];
			defaultSetLine[3] = tempBuffer[3];
			setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
		}	
	}

	@Override
	public void setDataBits(int dataBits) 
	{
		switch(dataBits)
		{
		case UsbSerialInterface.DATA_BITS_5:
			if(defaultSetLine[6] != 0x05)
			{
				defaultSetLine[6] = 0x05;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		case UsbSerialInterface.DATA_BITS_6:
			if(defaultSetLine[6] != 0x06)
			{
				defaultSetLine[6] = 0x06;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		case UsbSerialInterface.DATA_BITS_7:
			if(defaultSetLine[6] != 0x07)
			{
				defaultSetLine[6] = 0x07;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		case UsbSerialInterface.DATA_BITS_8:
			if(defaultSetLine[6] != 0x08)
			{
				defaultSetLine[6] = 0x08;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		}
		
	}

	@Override
	public void setStopBits(int stopBits)
	{
		switch(stopBits)
		{
		case UsbSerialInterface.STOP_BITS_1:
			if(defaultSetLine[4] != 0x00)
			{
				defaultSetLine[4] = 0x00;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		case UsbSerialInterface.STOP_BITS_15:
			if(defaultSetLine[4] != 0x01)
			{
				defaultSetLine[4] = 0x01;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		case UsbSerialInterface.STOP_BITS_2:
			if(defaultSetLine[4] != 0x02)
			{
				defaultSetLine[4] = 0x02;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
			}
			break;
		}
	}

	@Override
	public void setParity(int parity) 
	{
		switch(parity)
		{
		case UsbSerialInterface.PARITY_NONE:
			if(defaultSetLine[5] != 0x00)
			{
				defaultSetLine[5] = 0x00;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
			}
			break;
		case UsbSerialInterface.PARITY_ODD:
			if(defaultSetLine[5] != 0x01)
			{
				defaultSetLine[5] = 0x01;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
			}
			break;
		case UsbSerialInterface.PARITY_EVEN:
			if(defaultSetLine[5] != 0x02)
			{
				defaultSetLine[5] = 0x02;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
			}
			break;
		case UsbSerialInterface.PARITY_MARK:
			if(defaultSetLine[5] != 0x03)
			{
				defaultSetLine[5] = 0x03;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
			}
			break;
		case UsbSerialInterface.PARITY_SPACE:
			if(defaultSetLine[5] != 0x04)
			{
				defaultSetLine[5] = 0x04;
				setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
			}
			break;
		}
		
	}

	@Override
	public void setFlowControl(int flowControl)
	{
		// TODO
		
	}
	
	
	private int setControlCommand(int reqType ,int request, int value, int index, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
			dataLength = data.length;
		int response = connection.controlTransfer(reqType, request, value, index, data, dataLength, USB_TIMEOUT);
		Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
		return response;
	}


}