///////////////////////////////////////////
///   关于二维码扫描lib添加说明
///////////////////////////////////////////

1.调用方法   示例如下：
Intent intent = new Intent();
intent.setClass(YourActivity, CaptureActivity.class);
startActivityForResult(intent, REQUESTCODE);

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   super.onActivityResult(requestCode, resultCode, data);
   if (requestCode == REQUESTCODE) {
        if (resultCode == RESULT_OK && data != null) {
			String result = data.getStringExtra(CaptureActivity.RESULT_KEY);
			Log.i("test","扫描结果：" + result);
		} else {
			Log.e("test","no result");
		}
    }
}

2.在mainfest中的配置

    <!-- 扫描二维码权限声明 -->
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.CAMERA" />

    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
    

    <!-- 扫描二维码Activity -->
    <activity
            android:name="com.zxing.activity.CaptureActivity"
            android:theme="@android:style/Theme.NoTitleBar" />
    