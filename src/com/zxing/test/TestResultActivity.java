package com.zxing.test;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zxing.R;
import com.zxing.test.TestResultParse.VCard;
import com.zxing.test.TestResultParse.Wifi;
import com.zxing.test.TestWifiConnectUtil.WifiCipherType;

/**
 * @description 处理所有二维码扫描结果
 */
public class TestResultActivity extends Activity {

	/**
	 * 
	 * @description 定义二维码所有类型
	 * @version 1.0.0
	 * 
	 */
	public enum QRType {
		// *********
		// 二维码类型（空，微信，纯文本，网页，名片，wifi）
		// **********
		NULL, WECHAT, TEXT, WEB, VCARD, WIFI;

	}

	private final String TAG = TestResultActivity.class.getSimpleName();
	private Context context;
	// 纯文本显示
	private TextView text_layout;
	// http显示
	private WebView web_layout;
	// 名片显示
	private ScrollView vcard_layout;
	// 二维码扫描结果
	private String scanResult = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = TestResultActivity.this;
		setContentView(R.layout.test_activity_result);
		findViews();
	}

	private void findViews() {
		text_layout = (TextView) findViewById(R.id.textview_result);
		web_layout = (WebView) findViewById(R.id.webview_url);
		vcard_layout = (ScrollView) findViewById(R.id.vcard_layout);
		resultHandle();
	}

	/**
	 * 扫描结果分类处理 （TYPE:null,IM,纯文本，网页，微信，名片，wifi）
	 * 
	 * </br>结果格式示例:
	 * 
	 * </br> 纯文本：adnfgal1351
	 * 
	 * </br>网页：http://www.baidu.com
	 * 
	 * </br>微信：http://weixin.qq.com/r/...
	 * 
	 * </br>IM：http://im.vrv.cn/user/getinfo?...
	 * 
	 * </br>名片： BEGIN:VCARD /n.../n END:VCARD
	 * 
	 * </br> wifi：WIFI:T:WPA;S:VRV2103;P:vrv88447377;
	 * 
	 */
	private void resultHandle() {
		Intent intent = getIntent();
		scanResult = intent.getStringExtra(TestScanActivity.RESULT_KEY);
		Log.i(TAG, "二维码扫描结果：" + scanResult);
		QRType type = TestResultParse.parseQRType(scanResult);
		switch (type) {
		case NULL:// 扫描结果空
			Toast.makeText(context, "什么都没有", Toast.LENGTH_LONG).show();
			break;
		case WECHAT:// 微信二维码
			wechatHandle();
			break;
		case TEXT:// 纯文本
			textHandle();
			break;
		case WEB:// 网页
			webHandle();
			break;
		case VCARD:// 名片二维码
			vcardHandle();
			break;
		case WIFI:// wifi二维码
			wifiHandle();
			break;
		default:
			break;
		}

	}

	/**
	 * 微信结果处理
	 */
	private void wechatHandle() {

		// 弹出选择结果处理列表
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(scanResult));
		startActivityForResult(intent, 0);
	}

	/**
	 * 扫描结果 纯文本处理
	 */
	private void textHandle() {

		text_layout.setVisibility(View.VISIBLE);
		web_layout.setVisibility(View.GONE);
		vcard_layout.setVisibility(View.GONE);

		text_layout.setText(scanResult);
	}

	/**
	 * 扫描结果 http处理
	 */
	private void webHandle() {
		web_layout.setVisibility(View.VISIBLE);
		text_layout.setVisibility(View.GONE);
		vcard_layout.setVisibility(View.GONE);

		webViewSetting();
		web_layout.loadUrl(scanResult);
	}

	// 等待wifi连接dialog
	private AlertDialog wifiWaitDialog;
	private Wifi wifi;

	/**
	 * wifi结果处理
	 */
	private void wifiHandle() {
		wifi = TestResultParse.parseWifi(scanResult);
		if (wifi != null) {
			handler.sendEmptyMessageDelayed(WIFIRESULT_POPSHOWCODE, 500);
		} else {
			textHandle();
		}
	}

	/**
	 * 扫描结果 名片处理
	 */
	private void vcardHandle() {
		vcard_layout.setVisibility(View.VISIBLE);
		text_layout.setVisibility(View.GONE);
		web_layout.setVisibility(View.GONE);

		VCard vcard = TestResultParse.parseVcard(scanResult);
		if (vcard != null) {
			setVcardViews(vcard);
		} else {
			textHandle();
		}
	}

	private void showWifiPop(final Wifi wifi) {
		List<String> lists = new ArrayList<String>();
		lists.add("扫一扫为wifi二维码，是否连接" + wifi.getSSID() + " ?");
		lists.add("提示");

		Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("提示");
		dialog.setMessage("扫一扫为wifi二维码，是否连接" + wifi.getSSID() + " ?");
		dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				wifiWaitDialog = new AlertDialog.Builder(context)
						.setTitle("提示").setMessage("正在连接，请稍候").create();
				wifiWaitDialog.show();
				connectWifi(wifi);

			}
		});
		dialog.setNegativeButton("否", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.create().show();
	}

	// wifi 连接成功
	private final int CONNECT_OK = 0;
	// wifi 连接失败
	private final int CONNECT_FAIL = 1;
	// wifi 扫描结果pop延时展示code
	private final int WIFIRESULT_POPSHOWCODE = 10;
	// wifi 连接完成码
	private final int FINISHCODE = 11;
	// wifi连接过程是线程处理，连接完成后等待popview dismiss
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			int what = msg.what;
			if (what == FINISHCODE) {
				if (msg.arg1 == CONNECT_OK) {
					Toast.makeText(context, "Wifi 连接成功",Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, "wifi 连接失败",Toast.LENGTH_LONG).show();
				}
				if (wifiWaitDialog != null && wifiWaitDialog.isShowing()) {
					wifiWaitDialog.dismiss();
					finish();
				}

			} else if (what == WIFIRESULT_POPSHOWCODE) {
				showWifiPop(wifi);
			}
		};
	};

	/***
	 * 连接wifi，线程处理
	 * 
	 * @param wifi
	 */
	private void connectWifi(final Wifi wifi) {
		new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... params) {
				String SSID = wifi.getSSID();
				String password = wifi.getPassword();
				String typeString = wifi.getType();
				WifiCipherType type = null;
				if (TextUtils.isEmpty(password)
						|| TextUtils.isEmpty(typeString)) {
					type = WifiCipherType.WIFICIPHER_NOPASS;
				} else if (typeString.contains("WPA")) {
					type = WifiCipherType.WIFICIPHER_WPA;
				} else if (typeString.contains("WEP")) {
					type = WifiCipherType.WIFICIPHER_WEP;
				} else {
					type = WifiCipherType.WIFICIPHER_NOPASS;
				}
				TestWifiConnectUtil wifiConnect = new TestWifiConnectUtil(context);
				boolean isConnect = wifiConnect.connect(SSID, password, type);
				Log.e(TAG, "wifi connect:" + isConnect);
				Message message = new Message();
				message.what = FINISHCODE;
				if (isConnect) {
					message.arg1 = CONNECT_OK;
				} else {
					message.arg1 = CONNECT_FAIL;
				}
				handler.sendMessage(message);
				return null;
			}
		}.execute("");

	}

	/**
	 * 设置名片视图
	 * 
	 * @param vcard
	 */
	private void setVcardViews(final VCard vcard) {
		TextView nameTx = (TextView) findViewById(R.id.vcard_name);
		TextView telTx = (TextView) findViewById(R.id.vcard_tel);
		TextView cellTx = (TextView) findViewById(R.id.vcard_cell);
		TextView emailTx = (TextView) findViewById(R.id.vcard_email);
		TextView titleTx = (TextView) findViewById(R.id.vcard_title);
		TextView orgTx = (TextView) findViewById(R.id.vcard_org);
		TextView urlTx = (TextView) findViewById(R.id.vcard_url);
		TextView noteTx = (TextView) findViewById(R.id.vcard_note);
		// ImageView photoImg = (ImageView) findViewById(R.id.vcard_photo);
		Button saveBt = (Button) findViewById(R.id.vcard_saveBt);

		// TODO 网络加载头像图片 自己实现
		// photoImg.setImageURI(vcard.getPhoto());

		nameTx.setText(vcard.getName());
		telTx.setText(vcard.getTel());
		cellTx.setText(vcard.getCell());
		emailTx.setText(vcard.getEmail());
		titleTx.setText(vcard.getTitle());
		orgTx.setText(vcard.getOrg());
		urlTx.setText(vcard.getUrl());
		noteTx.setText(vcard.getNote());

		saveBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveVcard2Contacts(vcard);
			}
		});
	}

	/**
	 * 保存名片至通讯录
	 */
	private void saveVcard2Contacts(VCard vcard) {
		Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		intent.putExtra(Intents.Insert.NAME, vcard.getName());
		intent.putExtra(Intents.Insert.PHONE, vcard.getTel());
		intent.putExtra(Intents.Insert.EMAIL, vcard.getEmail());
		intent.putExtra(Intents.Insert.COMPANY, vcard.getOrg());
		intent.putExtra(Intents.Insert.JOB_TITLE, vcard.getTitle());
		intent.putExtra(Intents.Insert.NOTES, vcard.getNote());
		startActivity(intent);
	}

	/**
	 * webView设置
	 */
	@SuppressLint("SetJavaScriptEnabled")
	private void webViewSetting() {
		WebSettings settings = web_layout.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setUseWideViewPort(true);

		web_layout.setWebViewClient(new WebViewClient() {
			// 重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		// 添加二维码扫描后网页链接下载
		web_layout.setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
}
