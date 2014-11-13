package com.zxing.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.zxing.activity.CaptureActivity;

public class TestScanActivity extends Activity {

	private final int REQUESTCODE = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button button = new Button(this);
		button.setText("扫描二维码");
		setContentView(button);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(TestScanActivity.this, CaptureActivity.class);
				startActivityForResult(intent, REQUESTCODE);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUESTCODE) {
			if (resultCode == RESULT_OK && data != null) {
				String result = data.getStringExtra(CaptureActivity.RESULT_KEY);
				Log.i("Test", "扫描结果：" + result);
			} else {
				Log.e("Test", "no result");
			}
		}
	}

}
