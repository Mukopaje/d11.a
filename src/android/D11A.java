package cordova.plugins.d11a;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.util.Log;
import cordova.plugins.d11a.GlobalContants;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.widget.Toast;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.printer.sdk.PrinterConstants;
import com.printer.sdk.PrinterConstants.Connect;
import com.printer.sdk.PrinterInstance;
import com.printer.sdk.usb.USBPort;
import com.printer.sdk.utils.XLog;
/**
 * This class echoes a string called from JavaScript.
 */
public class D11A extends CordovaPlugin {

    private static PrinterInstance mPrinter;
    private static UsbDevice mUSBDevice;
    public static boolean isConnected = false;// 蓝牙连接状态
	public static String devicesName = "OCOM";
	private static String devicesAddress;
	private int printerId = 0;
	private int interfaceType = 0;
	private List<UsbDevice> deviceList;
	private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
    CordovaInterface cordovaInt;
    public String ReceiptContent;
    private Context context = null;
    private Context tContext = null;
    private Handler mHandler = null;
    public static final String TAG = "D11A";

    // private Handler mHandler = new Handler() {
    //   @SuppressLint("ShowToast")
    //   @Override
    //   public void handleMessage(Message msg) {
    //     switch (msg.what) {
    //     case Connect.SUCCESS:
    //       isConnected = true;
    //       GlobalContants.ISCONNECTED = isConnected;
    //       GlobalContants.DEVICENAME = devicesName;
    //       if (interfaceType == 0) {
    //         PrefUtils.setString(mContext, GlobalContants.DEVICEADDRESS, devicesAddress);
    //         bluDisconnectFilter = new IntentFilter();
    //         bluDisconnectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    //         context.registerReceiver(myReceiver, bluDisconnectFilter);
    //         hasRegDisconnectReceiver = true;
    //       }
    //       // TOTO 暂时将TSPL指令设置参数的设置放在这
    //       // if (setPrinterTSPL(myPrinter)) {
    //       // if (interfaceType == 0) {
    //       // Toast.makeText(mContext,
    //       // R.string.settingactivitty_toast_bluetooth_set_tspl_successful,
    //       // 0)
    //       // .show();
    //       // } else if (interfaceType == 1) {
    //       // Toast.makeText(mContext,
    //       // R.string.settingactivity_toast_usb_set_tspl_succefful,
    //       // 0).show();
    //       // }
    //       // }
    //       break;
    //     case Connect.FAILED:
    //       isConnected = false;
  
    //       Toast.makeText(mContext, R.string.conn_failed, Toast.LENGTH_SHORT).show();
    //       XLog.i(TAG, "ZL at SettingActivity Handler() 连接失败!");
    //       break;
    //     case Connect.CLOSED:
    //       isConnected = false;
    //       GlobalContants.ISCONNECTED = isConnected;
    //       GlobalContants.DEVICENAME = devicesName;
    //       Toast.makeText(mContext, R.string.conn_closed, Toast.LENGTH_SHORT).show();
    //       XLog.i(TAG, "ZL at SettingActivity Handler() 连接关闭!");
    //       break;
    //     case Connect.NODEVICE:
    //       isConnected = false;
    //       Toast.makeText(context, "No connection to printer device", Toast.LENGTH_SHORT).show();
    //       break;
    //     // case 10:
    //     // if (setPrinterTSPL(myPrinter)) {
    //     // Toast.makeText(mContext, "蓝牙连接设置TSPL指令成功", 0).show();
    //     // }
    //     default:
    //       break;
    //     }
  
    //     updateButtonState(isConnected);
  
    //     if (dialog != null && dialog.isShowing()) {
    //       dialog.dismiss();
    //     }
    //   }
  
    // };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "receiver action: " + action);
  
        if (ACTION_USB_PERMISSION.equals(action)) {
          synchronized (this) {
            context.unregisterReceiver(mUsbReceiver);
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                && mUSBDevice.equals(device)) {
              mPrinter.openConnection();
            } else {
              mHandler.obtainMessage(Connect.FAILED).sendToTarget();
              Log.e(TAG, "permission denied for device " + device);
            }
          }
        }
      }
    };


  @Override
  public boolean execute(
    String action,
    JSONArray data,
    CallbackContext callbackContext
  )
    throws JSONException {
    if (action.equals("printString")) {
      ReceiptContent = data.getString(0);
      new PrintReceipt().execute();
      return true;
    }
    // } else if (action.equals("printBarCode")) {
    // 	BarcodeData = data.getString(0);
    // 	 new PrintBarcode().execute();
    // return true;
    // } else if (action.equals("printQRCode")) {
    // 	QRCodeData = data.getString(0);
    // 	 new PrintQRCode().execute();
    // return true;
    // }
    return false;
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    tContext = webView.getContext();
    context = this.cordova.getActivity().getApplicationContext();
    mHandler = new Handler();

    // mUSBDevice = data.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		// 		mPrinter = PrinterInstance.getPrinterInstance(context, mUSBDevice, mHandler);
		// 		devicesName = mUSBDevice.getDeviceName();
		// 		devicesAddress = "vid: " + mUSBDevice.getVendorId() + "  pid: " + mUSBDevice.getProductId();
		// 		UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		// 		if (mUsbManager.hasPermission(mUSBDevice)) {
		// 			myPrinter.openConnection();
		// 		} else {
		// 			// 没有权限询问用户是否授予权限
		// 			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
		// 					new Intent(ACTION_USB_PERMISSION), 0);
		// 			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		// 			filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		// 			filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		// 			context.registerReceiver(mUsbReceiver, filter);
		// 			mUsbManager.requestPermission(mUSBDevice, pendingIntent); // 该代码执行后，系统弹出一个对话框
		// 		}

    UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    usbAutoConn(manager);
    //mPrinter = PrinterInstance.mPrinter;
   // Toast.makeText(tContext, mPrinter, Toast.LENGTH_LONG).show();
    // Log.d(TAG, "Initialization - "+ usbPrManger);

  }


	public void usbAutoConn(UsbManager manager) {

		doDiscovery(manager);
		if (deviceList.isEmpty()) {
			Toast.makeText(webView.getContext(), "No USB Printer Connected", 0).show();
			return;
		}
		mUSBDevice = deviceList.get(0);
		if (mUSBDevice == null) {
			mHandler.obtainMessage(Connect.FAILED).sendToTarget();
			return;
		}
		mPrinter = PrinterInstance.getPrinterInstance(context, mUSBDevice, mHandler);
		devicesName = mUSBDevice.getDeviceName();
		devicesAddress = "vid: " + mUSBDevice.getVendorId() + "  pid: " + mUSBDevice.getProductId();
		UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		if (mUsbManager.hasPermission(mUSBDevice)) {
			mPrinter.openConnection();
		} else {
			// 没有权限询问用户是否授予权限
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			context.registerReceiver(mUsbReceiver, filter);
			mUsbManager.requestPermission(mUSBDevice, pendingIntent); // 该代码执行后，系统弹出一个对话框
		}

    mPrinter = PrinterInstance.mPrinter;
	}

	private void doDiscovery(UsbManager manager) {
		HashMap<String, UsbDevice> devices = manager.getDeviceList();
		deviceList = new ArrayList<UsbDevice>();
		for (UsbDevice device : devices.values()) {
			if (USBPort.isUsbPrinter(device)) {
				deviceList.add(device);
			}
		}

	}


  public class PrintReceipt extends AsyncTask<Void, Void, String> {

    @Override
    protected void onPreExecute() {
      Toast
        .makeText(
          webView.getContext(),
          "Printing Please Wait... ",
          Toast.LENGTH_LONG
        )
        .show();
    }

    @Override
    protected String doInBackground(Void... params) {
      byte[] printContent1 = null;
      String s1 = null;
      try {
        printContent1 = strToByteArray(ReceiptContent, "UTF-8");
      } catch (UnsupportedEncodingException e2) {
        e2.printStackTrace();
      }

      try {
        System.out.println("content print started");
      
        mPrinter.sendBytesData(printContent1);
        System.out.println("content print finished");
      } catch (Exception e) {
        e.printStackTrace();
        Log.d(TAG, "Exception error =" + e);
        s1 = e.toString();
      }

      return s1;
    }
  }

  public static byte[] strToByteArray(String str) {
    if (str == null) {
      return null;
    }
    byte[] byteArray = str.getBytes();
    return byteArray;
  }

  public static byte[] strToByteArray(String str, String encodeStr)
    throws UnsupportedEncodingException {
    if (str == null) {
      return null;
    }
    byte[] byteArray = null;
    if (encodeStr.equals("IBM852")) {
      byteArray = str.getBytes("IBM852");
    } else if (encodeStr.equals("GB2312")) {
      byteArray = str.getBytes("GB2312");
    } else if (encodeStr.equals("ISO-8859-1")) {
      byteArray = str.getBytes("ISO-8859-1");
    } else if (encodeStr.equals("UTF-8")) {
      byteArray = str.getBytes("UTF-8");
    } else {
      byteArray = str.getBytes();
    }
    return byteArray;
  }

  public static String checkEncoding(String str) {
    if (str == null) {
      return null;
    }
    String encodestr = null;
    if (str.equals("1B7430")) {
      encodestr = "UTF-8";
    } else if (str.equals("1B7431")) {
      encodestr = "GB2312";
    } else if (str.equals("1B7412")) {
      encodestr = "IBM852";
    } else if (str.equals("1B7417")) {
      encodestr = "ISO-8859-1";
    } else {
      encodestr = "UTF-8";
    }
    return encodestr;
  }

  public static byte[] hexToBytes(String bcdStr) {
    byte[] retValue = new byte[(int) (bcdStr.length() / 2)];
    int k = 0;
    for (int i = 0; i < bcdStr.length(); i += 2) {
      retValue[(int) (i / 2)] =
        (byte) Integer.parseInt(bcdStr.substring(k, i + 2), 16);
      k += 2;
    }
    return retValue;
  }
}
