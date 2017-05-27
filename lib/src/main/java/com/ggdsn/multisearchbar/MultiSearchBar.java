package com.ggdsn.multisearchbar;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Created by LiaoXingyu on 15/03/2017.
 */

public class MultiSearchBar extends FrameLayout {

	private static final float MAX_SINGLE_SEARCH_DP_WIDTH = 240f;
	private OnModeChangedListener onModeChangedListener;
	private OnFocusChangeListener onFocusChangeListener1;
	private OnFocusChangeListener onFocusChangeListener2;
	private OnFocusChangeListener onFocusChangeListener3;
	/**
	 * 由于更改view的可见性会影响focus，所以当切换mode时会有好多focus change触发，用这个标记来适当屏蔽一些focus change事件
	 */
	private boolean permitFocus;

	public interface OnModeChangedListener {
		void onNewMode(Mode newMode);
	}

	public enum Type {
		One, Two, Three
	}

	public enum Mode {
		Normal, Input
	}

	private Type type;
	private Mode mode = Mode.Normal;
	private static final String TAG = "MultiSearchBar";
	private OnClickListener cancelButtonOnClickListener;

	private View layout;
	private AppCompatImageButton leftButton;
	private AppCompatImageButton searchButton;
	private AppCompatButton cancelButton;
	private AppCompatTextView titleText1;
	private AppCompatEditText searchEdit1;
	private AppCompatEditText searchEdit2;
	private AppCompatEditText searchEdit3;
	private View underLine;
	private OnClickListener leftButtonOnClick;
	private AppCompatTextView titleText2;
	private View midLine;
	private View midLine2;
	private View midLine3;
	private String title2;

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

	public void setCancelButtonOnClickListener(OnClickListener cancelButtonOnClickListener) {
		this.cancelButtonOnClickListener = cancelButtonOnClickListener;
	}

	public void addTextChangedListener1(TextWatcher watcher) {
		searchEdit1.addTextChangedListener(watcher);
	}

	public void removeTextChangedListener1(TextWatcher watcher) {
		searchEdit1.removeTextChangedListener(watcher);
	}

	public void addTextChangedListener2(TextWatcher watcher) {
		searchEdit2.addTextChangedListener(watcher);
	}

	public void removeTextChangedListener2(TextWatcher watcher) {
		searchEdit2.removeTextChangedListener(watcher);
	}

	public void addTextChangedListener3(TextWatcher watcher) {
		searchEdit3.addTextChangedListener(watcher);
	}

	public void removeTextChangedListener3(TextWatcher watcher) {
		searchEdit3.removeTextChangedListener(watcher);
	}

	public void setOnFocusChangeListener1(OnFocusChangeListener l) {
		onFocusChangeListener1 = l;
	}

	public void setOnFocusChangeListener2(OnFocusChangeListener l) {
		onFocusChangeListener2 = l;
	}

	public void setOnFocusChangeListener3(OnFocusChangeListener l) {
		onFocusChangeListener3 = l;
	}

	public void setTitle1(String title) {
		if (title == null) {
			return;
		}
		titleText1.setText(title);
	}

	public void setTitle2(String title) {
		if (title == null) {
			return;
		}
		title2 = title;
		titleText2.setText(title2);
		if (mode == Mode.Input) {
			return;
		}
		midLine.setVisibility(VISIBLE);
		titleText2.setVisibility(VISIBLE);
	}

	public Type getType() {
		return type;
	}

	public void switchMode(Mode newMode) {
		switchMode(newMode, true);
	}

	public void switchMode(Mode newMode, boolean withAnimation) {
		if (mode == newMode) {
			return;
		}

		switch (newMode) {
			case Normal:
				toNormalMode(true);
				break;
			case Input:
				toInputMode(true);
				break;
		}
	}

	public Mode getMode() {
		return mode;
	}

	public void setOnModeChangedListener(OnModeChangedListener listener) {
		onModeChangedListener = listener;
	}

	private void init(Context context, AttributeSet attrs) {
		layout = inflate(context, R.layout.multi_search_bar, this);
		leftButton = (AppCompatImageButton) layout.findViewById(R.id.multiSearchBarButtonLeft);
		searchButton = (AppCompatImageButton) layout.findViewById(R.id.multiSearchBarButtonSearch);
		cancelButton = (AppCompatButton) layout.findViewById(R.id.multiSearchBarButtonCancel);
		titleText1 = (AppCompatTextView) layout.findViewById(R.id.multiSearchBarTextViewTitle1);
		titleText2 = (AppCompatTextView) layout.findViewById(R.id.multiSearchBarTextViewTitle2);
		midLine = layout.findViewById(R.id.multiSearchBarMidLine);
		searchEdit1 = (AppCompatEditText) layout.findViewById(R.id.multiSearchBarEditTextSearch1);
		searchEdit2 = (AppCompatEditText) layout.findViewById(R.id.multiSearchBarEditTextSearch2);
		searchEdit3 = (AppCompatEditText) layout.findViewById(R.id.multiSearchBarEditTextSearch3);
		midLine2 = layout.findViewById(R.id.multiSearchBarMidLine2);
		midLine3 = layout.findViewById(R.id.multiSearchBarMidLine3);
		underLine = layout.findViewById(R.id.multiSearchBarLine);
		setupViews(attrs);
	}

	private void setupViews(AttributeSet attrs) {
		parseXml(attrs);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				toInputMode(true);
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (cancelButtonOnClickListener != null) {
					cancelButtonOnClickListener.onClick(v);
				}
				toNormalMode(true);
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

		FocusChangeListener focusChangeListener = new FocusChangeListener();
		searchEdit1.setOnFocusChangeListener(focusChangeListener);
		searchEdit2.setOnFocusChangeListener(focusChangeListener);
		searchEdit3.setOnFocusChangeListener(focusChangeListener);
	}

	private void toInputMode(boolean withAnimation) {
		// TODO: 22/03/2017 添加动画开关
		if (mode == Mode.Input) {
			return;
		}

		mode = Mode.Input;
		leftButton.setVisibility(INVISIBLE);
		cancelButton.setVisibility(VISIBLE);
		titleText1.setVisibility(INVISIBLE);
		midLine.setVisibility(GONE);
		titleText2.setVisibility(GONE);
		underLine.setVisibility(VISIBLE);

		ObjectAnimator.ofFloat(searchButton, TRANSLATION_X, -searchButton.getX()).start();
		TransitionDrawable drawable = (TransitionDrawable) searchButton.getDrawable();
		drawable.startTransition(100);

		AlphaAnimation searchEditAnim = new AlphaAnimation(0f, 1f);
		searchEditAnim.setDuration(300);

		AlphaAnimation searchBtnAnim = new AlphaAnimation(1f, 0f);
		searchBtnAnim.setDuration(300);
		searchBtnAnim.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {

			}

			@Override public void onAnimationEnd(Animation animation) {
				searchButton.setVisibility(GONE);
			}

			@Override public void onAnimationRepeat(Animation animation) {

			}
		});

		switch (type) {
			case One:
				midLine2.setVisibility(GONE);
				midLine3.setVisibility(GONE);
				searchEdit2.setVisibility(GONE);
				searchEdit3.setVisibility(GONE);

				searchEdit1.startAnimation(searchEditAnim);
				searchEdit1.setVisibility(VISIBLE);

				permitFocus = true;
				searchEdit1.requestFocus();
				showKeyboard(searchEdit1);
				break;
			case Two:
				searchButton.startAnimation(searchBtnAnim);
				leftButton.setVisibility(GONE);

				midLine3.setVisibility(GONE);
				searchEdit3.setVisibility(GONE);

				searchEdit1.startAnimation(searchEditAnim);
				searchEdit1.setVisibility(VISIBLE);

				midLine2.startAnimation(searchEditAnim);
				midLine2.setVisibility(VISIBLE);

				searchEdit2.setVisibility(VISIBLE);
				searchEdit2.startAnimation(searchEditAnim);
				// FIXME: 26/05/2017 改变最小长度，免得太短太难点
				permitFocus = true;
				searchEdit1.requestFocus();
				showKeyboard(searchEdit1);
				break;
			case Three:
				searchButton.startAnimation(searchBtnAnim);
				leftButton.setVisibility(GONE);

				searchEdit1.startAnimation(searchEditAnim);
				searchEdit1.setVisibility(VISIBLE);

				midLine2.setVisibility(VISIBLE);
				midLine2.startAnimation(searchEditAnim);
				midLine3.setVisibility(VISIBLE);
				midLine3.startAnimation(searchEditAnim);
				searchEdit2.setVisibility(VISIBLE);
				searchEdit2.startAnimation(searchEditAnim);
				searchEdit3.setVisibility(VISIBLE);
				searchEdit3.startAnimation(searchEditAnim);

				permitFocus = true;
				searchEdit1.requestFocus();
				showKeyboard(searchEdit1);
				break;
		}
		if (onModeChangedListener != null) {
			onModeChangedListener.onNewMode(mode);
		}
	}

	private void toNormalMode(boolean withAnimation) {

		if (mode == Mode.Normal) {
			return;
		}
		mode = Mode.Normal;

		View focus = layout.findFocus();
		if (focus != null) {
			focus.clearFocus();
		}
		permitFocus = false;

		leftButton.setVisibility(VISIBLE);
		underLine.setVisibility(GONE);
		cancelButton.setVisibility(INVISIBLE);
		titleText1.setVisibility(VISIBLE);
		if (!TextUtils.isEmpty(title2)) {
			midLine.setVisibility(VISIBLE);
			titleText2.setVisibility(VISIBLE);
		}
		searchEdit1.setVisibility(View.INVISIBLE);
		searchEdit2.setVisibility(GONE);
		searchEdit3.setVisibility(GONE);
		midLine2.setVisibility(GONE);
		midLine3.setVisibility(GONE);

		switch (type) {
			case Two:
			case Three:
				AlphaAnimation searchBtnAnim = new AlphaAnimation(0f, 1f);
				searchBtnAnim.setDuration(300);
				searchButton.setAnimation(searchBtnAnim);
				break;
		}
		searchButton.setVisibility(VISIBLE);

		ObjectAnimator.ofFloat(searchButton, TRANSLATION_X, 0).start();
		TransitionDrawable drawable = (TransitionDrawable) searchButton.getDrawable();
		drawable.reverseTransition(100);

		//layout.requestFocus();
		closeSoftKeyboard(layout);

		if (onModeChangedListener != null) {
			onModeChangedListener.onNewMode(mode);
		}
	}

	private class FocusChangeListener implements OnFocusChangeListener {

		@Override public void onFocusChange(View v, boolean hasFocus) {
			//Log.d(TAG, "onFocusChange() called with: v = [" + v + "], hasFocus = [" + hasFocus + "]");
			if (permitFocus) {
				int i = v.getId();
				if (i == R.id.multiSearchBarEditTextSearch1) {
					if (onFocusChangeListener1 == null) {
						return;
					}
					onFocusChangeListener1.onFocusChange(v, hasFocus);
				} else if (i == R.id.multiSearchBarEditTextSearch2) {
					if (onFocusChangeListener2 == null) {
						return;
					}
					onFocusChangeListener2.onFocusChange(v, hasFocus);
				} else if (i == R.id.multiSearchBarEditTextSearch3) {
					if (onFocusChangeListener3 == null) {
						return;
					}
					onFocusChangeListener3.onFocusChange(v, hasFocus);
				}
			}
			//if (type == Type.Three) {
			//	if (hasFocus) {
			//		AnimationListener listener;
			//		int width = v.getMeasuredWidth();
			//		if (searchEdit1 == v) {
			//			listener = new AnimationListener(width, v, searchEdit2, searchEdit3);
			//		} else if (searchEdit2 == v) {
			//			listener = new AnimationListener(width, v, searchEdit1, searchEdit3);
			//		} else {
			//			listener = new AnimationListener(width, v, searchEdit2, searchEdit1);
			//		}
			//		ValueAnimator anim = ValueAnimator.ofInt(width, width + 10);
			//		anim.addUpdateListener(listener);
			//		anim.setDuration(300);
			//		anim.start();
			//	} else {
			//		ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
			//		layoutParams.width -= increaseEdit1;
			//		v.setLayoutParams(layoutParams);
			//
			//		if (searchEdit1 == v) {
			//			layoutParams = searchEdit2.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit2.setLayoutParams(layoutParams);
			//
			//			layoutParams = searchEdit3.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit3.setLayoutParams(layoutParams);
			//		} else if (searchEdit2 == v) {
			//			layoutParams = searchEdit1.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit1.setLayoutParams(layoutParams);
			//
			//			layoutParams = searchEdit3.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit3.setLayoutParams(layoutParams);
			//		} else {
			//			layoutParams = searchEdit2.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit2.setLayoutParams(layoutParams);
			//
			//			layoutParams = searchEdit1.getLayoutParams();
			//			layoutParams.width += increaseEdit2;
			//			searchEdit1.setLayoutParams(layoutParams);
			//		}
			//
			//		increaseEdit1 = increaseEdit2 = 0;
			//	}
			//}
		}
	}

	private int increaseEdit1, increaseEdit2;

	private class AnimationListener implements ValueAnimator.AnimatorUpdateListener {
		private final int startVal;
		private View selected;
		private View other1;
		private View other2;

		public AnimationListener(int startVal, View selected, View other1, View other2) {
			this.startVal = startVal;
			this.selected = selected;
			this.other1 = other1;
			this.other2 = other2;
		}

		@Override public void onAnimationUpdate(ValueAnimator animation) {
			int val = (Integer) animation.getAnimatedValue();
			int diff = (val - startVal) / 2;
			ViewGroup.LayoutParams layoutParams = selected.getLayoutParams();
			layoutParams.width = val;
			selected.setLayoutParams(layoutParams);

			layoutParams = other1.getLayoutParams();
			layoutParams.width -= diff;
			other1.setLayoutParams(layoutParams);

			layoutParams = other2.getLayoutParams();
			layoutParams.width -= diff;
			other2.setLayoutParams(layoutParams);

			increaseEdit1 += val - startVal;
			increaseEdit2 -= diff;
		}
	}

	private void parseXml(AttributeSet attrs) {
		TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MultiSearchBar);

		type = Type.values()[typedArray.getInt(R.styleable.MultiSearchBar_multiSearchBarType, Type.One.ordinal())];

		int leftDrawableId = typedArray.getResourceId(R.styleable.MultiSearchBar_multiSearchBarLeftSrc, -1);
		if (leftDrawableId != -1) {
			leftButton.setImageResource(leftDrawableId);
		}

		String title = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle1);
		setTitle1(title);

		title2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle2);
		if (TextUtils.isEmpty(title2)) {
			midLine.setVisibility(GONE);
			titleText2.setVisibility(GONE);
		} else {
			titleText2.setText(title2);
		}

		String hint1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint1);
		if (hint1 != null) {
			searchEdit1.setHint(hint1);
		}

		int maxWidth = toPixels(getContext(), MAX_SINGLE_SEARCH_DP_WIDTH);
		switch (type) {
			case One:
				searchEdit1.setMaxWidth(maxWidth);
				break;
			case Two:
				searchEdit1.setMaxWidth(maxWidth/2 - 2);
				searchEdit2.setMaxWidth(maxWidth/2 - 2);
				searchEdit2.setImeOptions(EditorInfo.IME_ACTION_DONE);
				break;
		}

		String hint2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint2);
		if (hint2 != null) {
			searchEdit2.setHint(hint2);
		}
		String hint3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint3);
		if (hint3 != null) {
			searchEdit3.setHint(hint3);
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

	private static int toPixels(Context context, float dp) {
		final float scale = context.getResources().getDisplayMetrics().density;
		// Convert the dps to pixels, based on density scale
		//在转换时加上 0.5f，将该数字四舍五入到最接近的整数
		return (int) (dp * scale + 0.5f);
	}
}
