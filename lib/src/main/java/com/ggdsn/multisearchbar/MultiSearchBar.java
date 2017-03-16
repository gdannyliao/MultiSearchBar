package com.ggdsn.multisearchbar;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Created by LiaoXingyu on 15/03/2017.
 */

public class MultiSearchBar extends FrameLayout {
	private static final String TAG = "MultiSearchBar";
	private View layout;
	private AppCompatImageButton leftButton;
	private AppCompatImageButton searchButton;
	private AppCompatButton cancelButton;
	private AppCompatTextView titleText1;
	private AppCompatEditText searchEdit;
	private boolean isInInputMode = false;
	private View underLine;
	private OnClickListener leftButtonOnClick;
	private AppCompatTextView titleText2;
	private View midLine;

	public MultiSearchBar(@NonNull Context context) {
		super(context);
		init(context, null);
	}

	public MultiSearchBar(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public MultiSearchBar(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public void setLeftButtonOnClickListener(OnClickListener leftButtonOnClick) {
		this.leftButtonOnClick = leftButtonOnClick;
	}

	public void addTextChangedListener(TextWatcher watcher) {
		searchEdit.addTextChangedListener(watcher);
	}

	public void setTitle(String title) {
		if (title == null) {
			return;
		}
		titleText1.setText(title);
	}

	private void init(Context context, AttributeSet attrs) {
		layout = inflate(context, R.layout.simple_search_bar, this);
		leftButton = (AppCompatImageButton) layout.findViewById(R.id.simpleSearchBarButtonLeft);
		searchButton = (AppCompatImageButton) layout.findViewById(R.id.simpleSearchBarButtonSearch);
		cancelButton = (AppCompatButton) layout.findViewById(R.id.simpleSearchBarButtonCancel);
		titleText1 = (AppCompatTextView) layout.findViewById(R.id.simpleSearchBarTextViewTitle1);
		titleText2 = (AppCompatTextView) layout.findViewById(R.id.simpleSearchBarTextViewTitle2);
		midLine = layout.findViewById(R.id.simpleSearchBarMidLine);
		searchEdit = (AppCompatEditText) layout.findViewById(R.id.simpleSearchBarEditTextSearch);
		underLine = layout.findViewById(R.id.simpleSearchBarLine);
		setupViews(attrs);
	}

	private void setupViews(AttributeSet attrs) {
		parseXml(attrs);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (isInInputMode) {
					return;
				}

				isInInputMode = true;
				leftButton.setVisibility(INVISIBLE);
				cancelButton.setVisibility(VISIBLE);
				titleText1.setVisibility(INVISIBLE);
				midLine.setVisibility(INVISIBLE);
				titleText2.setVisibility(INVISIBLE);
				underLine.setVisibility(VISIBLE);

				ObjectAnimator.ofFloat(searchButton, "translationX", -searchButton.getX()).start();
				TransitionDrawable drawable = (TransitionDrawable) searchButton.getDrawable();
				drawable.startTransition(100);

				ObjectAnimator alpha = ObjectAnimator.ofFloat(searchEdit, "alpha", 0, 1);
				alpha.setStartDelay(100);
				alpha.start();

				searchEdit.setAlpha(0);
				searchEdit.setVisibility(VISIBLE);
				searchEdit.requestFocus();
				showKeyboard(searchEdit);
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (!isInInputMode) {
					return;
				}
				leftButton.setVisibility(VISIBLE);
				underLine.setVisibility(GONE);
				cancelButton.setVisibility(INVISIBLE);
				titleText1.setVisibility(VISIBLE);
				midLine.setVisibility(VISIBLE);
				titleText2.setVisibility(VISIBLE);
				searchEdit.setVisibility(View.INVISIBLE);

				ObjectAnimator.ofFloat(searchButton, "translationX", 0).start();
				TransitionDrawable drawable = (TransitionDrawable) searchButton.getDrawable();
				drawable.reverseTransition(100);

				layout.requestFocus();
				closeSoftKeyboard(layout);
				isInInputMode = false;
			}
		});

		leftButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (leftButtonOnClick == null) {
					Activity activity = getHostActivity();
					if (activity != null) {
						activity.onBackPressed();
					}
				} else {
					leftButtonOnClick.onClick(v);
				}
			}
		});
	}

	private void parseXml(AttributeSet attrs) {
		TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MultiSearchBar);

		String title = typedArray.getString(R.styleable.MultiSearchBar_simple_search_bar_title1);
		if (title != null) {
			setTitle(title);
		}

		String title2 = typedArray.getString(R.styleable.MultiSearchBar_simple_search_bar_title2);
		if (TextUtils.isEmpty(title2)) {
			midLine.setVisibility(GONE);
			titleText2.setVisibility(GONE);
		} else {
			titleText2.setText(title2);
		}

		String hint = typedArray.getString(R.styleable.MultiSearchBar_simple_search_bar_hint);
		if (hint != null) {
			searchEdit.setHint(hint);
		}

		typedArray.recycle();
	}

	private void showKeyboard(EditText editText) {
		InputMethodManager inputMethodManager =
			(InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
	}

	private Activity getHostActivity() {
		Context context = getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof Activity) {
				return (Activity) context;
			}
			context = ((ContextWrapper) context).getBaseContext();
		}
		return null;
	}

	private void closeSoftKeyboard(View focused) {
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
	}
}
