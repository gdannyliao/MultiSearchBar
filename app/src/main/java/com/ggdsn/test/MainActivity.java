package com.ggdsn.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import com.ggdsn.multisearchbar.MultiSearchBar;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {

	private static final String TAG = "Main";

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MultiSearchBar searchBar1 = (MultiSearchBar) findViewById(R.id.title1);
		searchBar1.setOnFocusChangeListener1(this);
		searchBar1.setOnFocusChangeListener2(this);
		searchBar1.setOnFocusChangeListener3(this);

		searchBar1.setTitle2("北京国际财经中心周边的智能餐厅");

		AppCompatEditText editText = (AppCompatEditText) searchBar1.findViewById(R.id.multiSearchBarEditTextSearch3);
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					Log.d(TAG, "onEditorAction() called with: v = ["
						+ v
						+ "], actionId = ["
						+ actionId
						+ "], event = ["
						+ event
						+ "]");
				}
				return false;
			}
		});
	}

	@Override public void onFocusChange(View v, boolean hasFocus) {
		Log.d(TAG, "onFocusChange() called with: v = [" + v + "], hasFocus = [" + hasFocus + "]");
	}
}
