package com.zxing.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.Log;

import com.zxing.test.TestResultActivity.QRType;


/**
 * 
 * @description 二维码结果解析
 * @date 2014年5月14日
 * @author Yang
 * @version 1.0.0
 * 
 */
public class TestResultParse {

    private static final String TAG = TestResultParse.class.getSimpleName();

    // 微信包名
    private static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    // 微信二维码域名
    private static final String WECHAT_SERVER = "http://weixin.qq.com";

    /* 名片二维码起始标识 */
    private static final String VCARD_START = "BEGIN:VCARD\n";
    /* 名片二维码结尾标识 */
    private static final String VCARD_END = "\nEND:VCARD";

    /* Wifi二维码起始标识 */
    private static final String WIFI_START = "WIFI:";
    /* wifi二维码结尾标识 */
    private static final String WIFI_END = ";";

    /**
     * 解析二维码类型 ,返回类型参考{@link QRType}
     * 
     * @param scanResult
     * @return
     */
    public static QRType parseQRType(String scanResult) {

        if (null==scanResult||scanResult.length()<=0) {
            return QRType.NULL;
		} else if (scanResult.startsWith(WECHAT_SERVER)) {
            return QRType.WECHAT;
        }else if (scanResult.startsWith("http")) {
            return QRType.WEB;
        } else if (isFLag(scanResult, VCARD_START, VCARD_END)) {
            return QRType.VCARD;
        } else if (isFLag(scanResult, WIFI_START, WIFI_END)) {
            return QRType.WIFI;
        }else {
            return QRType.TEXT;
        }
    }

    /**
     * 判断手机是否安装微信
     * 
     * @return
     */
    public boolean isInstallWechat(Context context) {

        // 获取所有已安装程序的包信息
        List<PackageInfo> pinfo = context.getPackageManager().getInstalledPackages(0);
        // 所有已安装程序的包名
        List<String> pNames = new ArrayList<String>();
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                pNames.add(pn);
            }
        }
        // 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
        return pNames.contains(WECHAT_PACKAGENAME);
    }

    /**
     * 判断扫描结果是否为 某一特殊二维码
     * 
     * @param scanResult
     * @param start 特殊二维码起始标识
     * @param end 特殊二维码结尾标识
     * @return
     */
    private static boolean isFLag(String scanResult, String start, String end) {
        int startIndex = scanResult.indexOf(start);
        int endIndex = scanResult.indexOf(end);
        if (endIndex > startIndex) {
            return true;
        }
        return false;
    }

    /**
     * 截取二维码名片信息
     * 
     * @param scanResult
     * 
     * @return
     */
    private static String subVcardInfo(String scanResult) {
        if (TextUtils.isEmpty(scanResult)) {
            return null;
        }
        int startIndex = scanResult.indexOf(VCARD_START);
        int endIndex = scanResult.indexOf(VCARD_END);
        if (endIndex > startIndex) {
            String vcardResult = scanResult.substring(startIndex + VCARD_START.length(), endIndex);
            return vcardResult;
        }
        return null;
    }

    /**
     * 截取wifi信息
     * 
     * @param scanResult
     * @return
     */
    private static String subWifiInfo(String scanResult) {
        if (TextUtils.isEmpty(scanResult)) {
            return null;
        }
        int startIndex = scanResult.indexOf(WIFI_START);
        String wifiResult = scanResult.substring(startIndex + WIFI_START.length());
        return wifiResult;
    }

    /**
     * 解析vCard扫描结果
     * 
     * @param scanResult
     * @return
     */
    public static VCard parseVcard(String scanResult) {
        String vcardResult = subVcardInfo(scanResult);
        if (TextUtils.isEmpty(vcardResult)) {
            return null;
        }
		Log.i(TAG, "名片信息：" + vcardResult);
        StringReader stringReader = new StringReader(vcardResult);
        BufferedReader reader = new BufferedReader(stringReader);
        String temp = null;
        VCard card = new TestResultParse().new VCard();
        try {
            while ((temp = reader.readLine()) != null) {
                setVcard(card, temp);
            }
        } catch (Exception e) {
			Log.e(TAG, e.getMessage());

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return card;
    }

    private static void setVcard(VCard vcard, String vcardResult) {
        final String split = ":";
        if (null == vcardResult || !vcardResult.contains(split)) {
            return;
        }
        String[] sperators = vcardResult.split(split);
        String key = sperators[0];
        String value = vcardResult.substring(key.length() + 1);
        if (key.equalsIgnoreCase("VERSION")) {
            vcard.setVersion(value);
        } else if (key.equalsIgnoreCase("PHOTO")) {
            vcard.setPhoto(value);
        } else if (key.equalsIgnoreCase("N") || key.equalsIgnoreCase("FN")) {
            vcard.setName(value);
        } else if (key.equalsIgnoreCase("TEL")) {
            vcard.setTel(value);
        } else if (key.equalsIgnoreCase("EMAIL")) {
            vcard.setEmail(value);
        } else if (key.equalsIgnoreCase("TITLE")) {
            vcard.setTitle(value);
        } else if (key.equalsIgnoreCase("ORG")) {
            vcard.setOrg(value);
        } else if (key.equalsIgnoreCase("TEL;CELL")) {
            vcard.setCell(value);
        } else if (key.equalsIgnoreCase("URL")) {
            vcard.setUrl(value);
        } else if (key.equalsIgnoreCase("NOTE")) {
            vcard.setNote(value);
        }
    }

    /**
     * 解析wifi信息
     * 
     * @param scanResult
     * @return
     */
    public static Wifi parseWifi(String scanResult) {
        String wifiResult = subWifiInfo(scanResult);
        final String split = ";";
        if (TextUtils.isEmpty(wifiResult)) {
            return null;
        }
        if (!wifiResult.contains(split)) {
            return null;
        }
		Log.i(TAG, "wifi信息：" + wifiResult);
        String[] sperators = wifiResult.split(";");
        Wifi wifi = new TestResultParse().new Wifi();
        for (int i = 0; i < sperators.length; i++) {
            setWifi(wifi, sperators[i]);
        }
        return wifi;
    }

    private static void setWifi(Wifi wifi, String wifiResult) {
        final String split = ":";
        if (null == wifiResult || !wifiResult.contains(split)) {
            return;
        }
        String[] sperators = wifiResult.split(split);
        String key = sperators[0];
        String value = sperators[1];
        if (key.equalsIgnoreCase("S")) {
            wifi.setSSID(value);
        } else if (key.equalsIgnoreCase("P")) {
            wifi.setPassword(value);
        } else if (key.equalsIgnoreCase("T")) {
            wifi.setType(value);
        }
    }

    public class VCard {
        private String version;
        private String name;
        private String photo;// 头像地址
        private String tel;
        private String cell;// 电话
        private String email;
        private String org;// 单位组织
        private String title;// 职位
        private String url;// 主页
        private String note;// 备注

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }

        public String getTel() {
            return tel;
        }

        public void setTel(String tel) {
            this.tel = tel;
        }

        public String getCell() {
            return cell;
        }

        public void setCell(String cell) {
            this.cell = cell;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOrg() {
            return org;
        }

        public void setOrg(String org) {
            this.org = org;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

    }
    
    public class Wifi {
        private String SSID;// wifi名称
        private String password;// wifi 密码
        private String type;// Wifi加密类型


        public String getSSID() {
            return SSID;
        }

        public void setSSID(String sSID) {
            SSID = sSID;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }


    }


}
