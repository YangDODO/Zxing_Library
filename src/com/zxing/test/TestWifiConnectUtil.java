package com.zxing.test;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * @description wifi 连接工具类，扫描wifi后连接
 * @date 2014年5月15日
 * @version 1.0.0
 * 
 */
public class TestWifiConnectUtil {

    private final String TAG = TestWifiConnectUtil.class.getSimpleName();
    private WifiManager wifiManager;

	// 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
	public enum WifiCipherType {
		WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
	}

	public TestWifiConnectUtil(Context context) {
		wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
	}

	// 打开wifi功能
	private boolean openWifi() {
		boolean bRet = true;
		if (!wifiManager.isWifiEnabled()) {
			Log.i(TAG, "===>打开Wifi连接...");
			bRet = wifiManager.setWifiEnabled(true);
		}
		return bRet;
	}

	// 提供一个外部接口，传入要连接的无线网
    public boolean connect(String SSID, String password, WifiCipherType type) {
		if (!this.openWifi()) {
			return false;
		}
        // 开启wifi功能需要一段时间，所以要等到wifi
		// 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
		while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
			try {
				Log.i(TAG, "===>wifi连接状态开启等待中...");
				// 为了避免程序一直while循环，让它睡个100毫秒在检测……
				Thread.currentThread();
				Thread.sleep(100);
			} catch (InterruptedException ie) {
			}
		}
        // 扫描wifi,判断要连接的网络是否在当前扫描结果中
        /*if (!scanWifiIsExist(SSID)) {
            ImLog.e(TAG, SSID + "网络不在范围内！");
            return false;
        }*/

        // 判断当前连接的wifi是否是准备连接的wifi
        boolean isConnect = currentWifiIsSSID(SSID);
        if (isConnect) {
            return true;
        }
        // 不是断开当前连接的wifi
        disConnectCurrentWifi();

        WifiConfiguration wifiConfig = createWifiInfo(SSID, password, type);
		//
        if (wifiConfig == null) {
			Log.i(TAG, "===>wificonfig is null");
			return false;
		}

		WifiConfiguration tempConfig = this.isExsits(SSID);
		if (tempConfig != null) {
			Log.i(TAG, "remove 以前手机中" + SSID + "Wifi 配置");
			wifiManager.removeNetwork(tempConfig.networkId);
		}

		int netID = wifiManager.addNetwork(wifiConfig);
        boolean bRet = wifiManager.enableNetwork(netID, true);
		return bRet;
	}

    /**
     * 扫描需要创建的wifi是否存在
     * 
     * @param SSID
     * @return
     */
    private boolean scanWifiIsExist(String SSID) {
        if (wifiManager.startScan()) { // 扫描可用的无线网络
            List<ScanResult> scanResultList = wifiManager.getScanResults();
			Log.e(TAG, "scanresult size:" + scanResultList.size());
            for (int i = 0; i < scanResultList.size(); i++) {
                ScanResult scanRet = scanResultList.get(i);
				Log.e(TAG, "scan SSID:" + scanRet.SSID);
                if (SSID.equals(scanRet.SSID.replace("\"", ""))) // 找到要连接的SSID
                    return true;
            }
        }
        return false;
    }

    /**
     * 判断当前连接的wifi是否为需要连接的网络
     * 
     * @param SSID
     * @return
     */
    private boolean currentWifiIsSSID(String SSID) {
		Log.i(TAG, "检查当前网络是否为要连接的网络");
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && null != wifiInfo.getSSID()) {
            return SSID.equals(wifiInfo.getSSID().replace("\"", ""));
        }
        return false;
    }

    private void disConnectCurrentWifi() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
			Log.i(TAG, "===>断开当前连接网络");
            int netId = wifiInfo.getNetworkId();
			Log.e(TAG, "netid:" + netId);
			Log.e(TAG, "SSID:" + wifiInfo.getSSID());
			Log.e(TAG, "BSSID:" + wifiInfo.getBSSID());
            wifiManager.disableNetwork(netId);
            wifiManager.disconnect();
            wifiInfo = null;
        }
    }

	// 查看以前是否也配置过这个网络
    private WifiConfiguration isExsits(String SSID) {
		List<WifiConfiguration> existingConfigs = wifiManager
				.getConfiguredNetworks();
		for (WifiConfiguration existingConfig : existingConfigs) {
			if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
				return existingConfig;
			}
		}
		return null;
	}

	private WifiConfiguration createWifiInfo(String SSID, String password,
			WifiCipherType type) {
		Log.i(TAG, "===>wifi 创建：" + SSID + "/" + type);
		WifiConfiguration config = new WifiConfiguration();
		config.allowedAuthAlgorithms.clear();
		config.allowedGroupCiphers.clear();
		config.allowedKeyManagement.clear();
		config.allowedPairwiseCiphers.clear();
		config.allowedProtocols.clear();
		config.SSID = "\"" + SSID + "\"";
		if (type == WifiCipherType.WIFICIPHER_NOPASS) {
			config.wepKeys[0] = "";
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.wepTxKeyIndex = 0;
        } else if (type == WifiCipherType.WIFICIPHER_WEP) {
			config.preSharedKey = "\"" + password + "\"";
			config.hiddenSSID = true;
			config.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.SHARED);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			config.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP104);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.wepTxKeyIndex = 0;
        } else if (type == WifiCipherType.WIFICIPHER_WPA) {
			config.preSharedKey = "\"" + password + "\"";
			config.hiddenSSID = true;
			config.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.status = WifiConfiguration.Status.ENABLED;
		} else {
			return null;
		}
		return config;
	}

}
