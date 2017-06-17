package com.ggdsn.multisearchbar

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.support.annotation.AttrRes
import android.support.v7.widget.*
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupWindow

/**
 * Created by LiaoXingyu on 15/03/2017.
 */

class MultiSearchBar : FrameLayout {
    private var onModeChangedListener: OnModeChangedListener? = null
    private var onFocusChangeListener1: View.OnFocusChangeListener? = null
    private var onFocusChangeListener2: View.OnFocusChangeListener? = null
    private var onFocusChangeListener3: View.OnFocusChangeListener? = null
    /**
     * 由于更改view的可见性会影响focus，所以当切换mode时会有好多focus change触发，用这个标记来适当屏蔽一些focus change事件
     */
    private var permitFocus: Boolean = false
    private var hint1: String? = null
    private var hint2: String? = null
    private var hint3: String? = null

    interface OnModeChangedListener {
        fun onNewMode(newMode: Mode)
    }

    enum class Type {
        One, Two, Three, Popup
    }

    enum class Mode {
        Normal, Input
    }

    var type: Type? = null
        private set
    var mode = Mode.Normal
        private set
    private var cancelButtonOnClickListener: View.OnClickListener? = null

    private var layout: View? = null
    private var leftButton: AppCompatImageButton? = null
    private var searchButton: AppCompatImageButton? = null
    private var cancelButton: AppCompatButton? = null
    private var titleText1: AppCompatTextView? = null
    private var searchEdit1: AppCompatEditText? = null
    private var searchEdit2: AppCompatEditText? = null
    private var searchEdit3: AppCompatEditText? = null
    private var underLine: View? = null
    private var leftButtonOnClick: View.OnClickListener? = null
    private var titleText2: AppCompatTextView? = null
    private var midLine: View? = null
    private var midLine2: View? = null
    private var midLine3: View? = null
    private var title2: String? = null
    private var searchDefaultDrawable: TransitionDrawable? = null

    private var popupDrawable1: Drawable? = null
    private var popupDrawable2: Drawable? = null
    private var popupDrawable3: Drawable? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    fun setLeftButtonOnClickListener(leftButtonOnClick: View.OnClickListener) {
        this.leftButtonOnClick = leftButtonOnClick
    }

    fun setCancelButtonOnClickListener(cancelButtonOnClickListener: View.OnClickListener) {
        this.cancelButtonOnClickListener = cancelButtonOnClickListener
    }

    fun addTextChangedListener1(watcher: TextWatcher) {
        searchEdit1!!.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener1(watcher: TextWatcher) {
        searchEdit1!!.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener2(watcher: TextWatcher) {
        searchEdit2!!.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener2(watcher: TextWatcher) {
        searchEdit2!!.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener3(watcher: TextWatcher) {
        searchEdit3!!.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener3(watcher: TextWatcher) {
        searchEdit3!!.removeTextChangedListener(watcher)
    }

    fun setOnFocusChangeListener1(l: View.OnFocusChangeListener) {
        onFocusChangeListener1 = l
    }

    fun setOnFocusChangeListener2(l: View.OnFocusChangeListener) {
        onFocusChangeListener2 = l
    }

    fun setOnFocusChangeListener3(l: View.OnFocusChangeListener) {
        onFocusChangeListener3 = l
    }

    fun setTitle1(title: String?) {
        if (title == null) {
            return
        }
        titleText1!!.text = title
    }

    fun setTitle2(title: String?) {
        if (title == null) {
            return
        }
        title2 = title
        titleText2!!.text = title2
        if (mode == Mode.Input) {
            return
        }
        midLine!!.visibility = View.VISIBLE
        titleText2!!.visibility = View.VISIBLE
    }

    @JvmOverloads fun switchMode(newMode: Mode, withAnimation: Boolean = true) {
        if (mode == newMode) {
            return
        }

        when (newMode) {
            MultiSearchBar.Mode.Normal -> toNormalMode(true)
            MultiSearchBar.Mode.Input -> toInputMode(true)
        }
    }

    fun setOnModeChangedListener(listener: OnModeChangedListener) {
        onModeChangedListener = listener
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        layout = View.inflate(context, R.layout.multi_search_bar, this)
        leftButton = layout!!.findViewById(R.id.multiSearchBarButtonLeft) as AppCompatImageButton
        searchButton = layout!!.findViewById(R.id.multiSearchBarButtonSearch) as AppCompatImageButton
        searchDefaultDrawable = searchButton!!.drawable as TransitionDrawable?

        cancelButton = layout!!.findViewById(R.id.multiSearchBarButtonCancel) as AppCompatButton
        titleText1 = layout!!.findViewById(R.id.multiSearchBarTextViewTitle1) as AppCompatTextView
        titleText2 = layout!!.findViewById(R.id.multiSearchBarTextViewTitle2) as AppCompatTextView
        midLine = layout!!.findViewById(R.id.multiSearchBarMidLine)
        searchEdit1 = layout!!.findViewById(R.id.multiSearchBarEditTextSearch1) as AppCompatEditText
        searchEdit2 = layout!!.findViewById(R.id.multiSearchBarEditTextSearch2) as AppCompatEditText
        searchEdit3 = layout!!.findViewById(R.id.multiSearchBarEditTextSearch3) as AppCompatEditText
        midLine2 = layout!!.findViewById(R.id.multiSearchBarMidLine2)
        midLine3 = layout!!.findViewById(R.id.multiSearchBarMidLine3)
        underLine = layout!!.findViewById(R.id.multiSearchBarLine)
        setupViews(attrs)
    }

    private var popupWindow: PopupWindow? = null

    private fun setupViews(attrs: AttributeSet?) {
        attrs?.let {
            parseXml(attrs)
        }
        searchButton!!.setOnClickListener(View.OnClickListener { v ->
            if (mode == Mode.Input) {
                if (type == Type.Popup) {
                    popupWindow?.showAsDropDown(searchButton)
                }
                return@OnClickListener
            }
            toInputMode(true)
        })

        cancelButton!!.setOnClickListener { v ->
            cancelButtonOnClickListener?.onClick(v)
            toNormalMode(true)
        }

        leftButton!!.setOnClickListener { v ->
            if (leftButtonOnClick == null) {
                val activity = hostActivity
                activity?.onBackPressed()
            } else {
                leftButtonOnClick!!.onClick(v)
            }
        }

        val focusChangeListener = FocusChangeListener()
        searchEdit1!!.onFocusChangeListener = focusChangeListener
        searchEdit2!!.onFocusChangeListener = focusChangeListener
        searchEdit3!!.onFocusChangeListener = focusChangeListener

        if (type == Type.Popup) {
            val view = LayoutInflater.from(context).inflate(R.layout.multi_search_bar_popup, null)
            if (popupDrawable1 == null) {
                view.findViewById(R.id.multiSearchBarPopupDrawable1).visibility = View.GONE
            } else {
                val imageView1 = view.findViewById(R.id.multiSearchBarPopupDrawable1) as AppCompatImageView
                imageView1.setImageDrawable(popupDrawable1)
            }

            val textView1 = view.findViewById(R.id.multiSearchBarPopupText1) as AppCompatTextView
            textView1.text = popupText1

            if (popupDrawable2 == null) {
                view.findViewById(R.id.multiSearchBarPopupDrawable2).visibility = View.GONE
            } else {
                val imageView2 = view.findViewById(R.id.multiSearchBarPopupDrawable2) as AppCompatImageView
                imageView2.setImageDrawable(popupDrawable2)
            }

            val textView2 = view.findViewById(R.id.multiSearchBarPopupText2) as AppCompatTextView
            textView2.text = popupText2

            if (popupDrawable3 == null) {
                view.findViewById(R.id.multiSearchBarPopupDrawable3).visibility = View.GONE
            } else {
                val imageView3 = view.findViewById(R.id.multiSearchBarPopupDrawable2) as AppCompatImageView
                imageView3.setImageDrawable(popupDrawable2)
            }
            val textView3 = view.findViewById(R.id.multiSearchBarPopupText3) as AppCompatTextView
            textView3.text = popupText3

            popupWindow = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow!!.isOutsideTouchable = true
            popupWindow!!.setBackgroundDrawable(ColorDrawable(0x00000000))
        }
    }

    private fun toInputMode(withAnimation: Boolean) {
        // TODO: 22/03/2017 添加动画开关
        if (mode == Mode.Input) {
            return
        }

        mode = Mode.Input
        leftButton!!.visibility = View.INVISIBLE
        cancelButton!!.visibility = View.VISIBLE
        titleText1!!.visibility = View.INVISIBLE
        midLine!!.visibility = View.GONE
        titleText2!!.visibility = View.GONE
        underLine!!.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(searchButton, View.TRANSLATION_X, -searchButton!!.x).start()
        val drawable = searchButton!!.drawable as TransitionDrawable
        drawable.startTransition(100)

        val searchEditAnim = AlphaAnimation(0f, 1f)
        searchEditAnim.duration = 300

        val searchBtnAnim = AlphaAnimation(1f, 0f)
        searchBtnAnim.duration = 300
        searchBtnAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                searchButton?.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        when (type) {
            MultiSearchBar.Type.Popup -> {
                searchEdit1!!.hint = hint1
                popupDrawable1?.let { searchButton?.setImageDrawable(popupDrawable1) }

                midLine2!!.visibility = View.GONE
                midLine3!!.visibility = View.GONE
                searchEdit2!!.visibility = View.GONE
                searchEdit3!!.visibility = View.GONE

                searchEdit1!!.startAnimation(searchEditAnim)
                searchEdit1!!.visibility = View.VISIBLE

                permitFocus = true
                searchEdit1!!.requestFocus()
                showKeyboard(searchEdit1!!)
            }
            MultiSearchBar.Type.One -> {
                midLine2!!.visibility = View.GONE
                midLine3!!.visibility = View.GONE
                searchEdit2!!.visibility = View.GONE
                searchEdit3!!.visibility = View.GONE
                searchEdit1!!.startAnimation(searchEditAnim)
                searchEdit1!!.visibility = View.VISIBLE
                permitFocus = true
                searchEdit1!!.requestFocus()
                showKeyboard(searchEdit1!!)
            }
            MultiSearchBar.Type.Two -> {
                searchButton!!.startAnimation(searchBtnAnim)
                leftButton!!.visibility = View.GONE

                midLine3!!.visibility = View.GONE
                searchEdit3!!.visibility = View.GONE

                searchEdit1!!.startAnimation(searchEditAnim)
                searchEdit1!!.visibility = View.VISIBLE

                midLine2!!.startAnimation(searchEditAnim)
                midLine2!!.visibility = View.VISIBLE

                searchEdit2!!.visibility = View.VISIBLE
                searchEdit2!!.startAnimation(searchEditAnim)
                // FIXME: 26/05/2017 改变最小长度，免得太短太难点
                permitFocus = true
                searchEdit1!!.requestFocus()
                showKeyboard(searchEdit1!!)
            }
            MultiSearchBar.Type.Three -> {
                searchButton!!.startAnimation(searchBtnAnim)
                leftButton!!.visibility = View.GONE

                searchEdit1!!.startAnimation(searchEditAnim)
                searchEdit1!!.visibility = View.VISIBLE

                midLine2!!.visibility = View.VISIBLE
                midLine2!!.startAnimation(searchEditAnim)
                midLine3!!.visibility = View.VISIBLE
                midLine3!!.startAnimation(searchEditAnim)
                searchEdit2!!.visibility = View.VISIBLE
                searchEdit2!!.startAnimation(searchEditAnim)
                searchEdit3!!.visibility = View.VISIBLE
                searchEdit3!!.startAnimation(searchEditAnim)

                permitFocus = true
                searchEdit1!!.requestFocus()
                showKeyboard(searchEdit1!!)
            }
        }
        onModeChangedListener?.onNewMode(mode)
    }

    private fun toNormalMode(withAnimation: Boolean) {
        if (mode == Mode.Normal) {
            return
        }
        mode = Mode.Normal

        val focus = layout!!.findFocus()
        focus?.clearFocus()
        permitFocus = false

        leftButton!!.visibility = View.VISIBLE
        underLine!!.visibility = View.GONE
        cancelButton!!.visibility = View.INVISIBLE
        titleText1!!.visibility = View.VISIBLE
        if (!TextUtils.isEmpty(title2)) {
            midLine!!.visibility = View.VISIBLE
            titleText2!!.visibility = View.VISIBLE
        }
        searchEdit1!!.visibility = View.INVISIBLE
        searchEdit2!!.visibility = View.GONE
        searchEdit3!!.visibility = View.GONE
        midLine2!!.visibility = View.GONE
        midLine3!!.visibility = View.GONE

        when (type) {
            MultiSearchBar.Type.Two, MultiSearchBar.Type.Three -> {
                val searchBtnAnim = AlphaAnimation(0f, 1f)
                searchBtnAnim.duration = 300
                searchButton!!.animation = searchBtnAnim
                ObjectAnimator.ofFloat<View>(searchButton, View.TRANSLATION_X, 0f).start()
                val drawable = searchButton!!.drawable as TransitionDrawable
                drawable.reverseTransition(100)
            }
            MultiSearchBar.Type.One, MultiSearchBar.Type.Popup -> {
                searchButton!!.setImageDrawable(searchDefaultDrawable)
                ObjectAnimator.ofFloat<View>(searchButton, View.TRANSLATION_X, 0f).start()
                searchDefaultDrawable?.reverseTransition(100)
            }
        }
        searchButton!!.visibility = View.VISIBLE

        closeSoftKeyboard(layout!!)

        onModeChangedListener?.onNewMode(mode)
    }

    private inner class FocusChangeListener : View.OnFocusChangeListener {

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (permitFocus) {
                when (v.id) {
                    R.id.multiSearchBarEditTextSearch1 -> onFocusChangeListener1?.onFocusChange(v, hasFocus)
                    R.id.multiSearchBarEditTextSearch2 -> onFocusChangeListener2?.onFocusChange(v, hasFocus)
                    R.id.multiSearchBarEditTextSearch3 -> onFocusChangeListener3?.onFocusChange(v, hasFocus)
                }
            }
        }
    }

    private var popupText1: String? = null
    private var popupText2: String? = null
    private var popupText3: String? = null

    private fun parseXml(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiSearchBar)

        type = Type.values()[typedArray.getInt(R.styleable.MultiSearchBar_multiSearchBarType, Type.One.ordinal)]

        val leftDrawableId = typedArray.getResourceId(R.styleable.MultiSearchBar_multiSearchBarLeftSrc, -1)
        if (leftDrawableId != -1) {
            leftButton!!.setImageResource(leftDrawableId)
        }

        val title = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle1)
        setTitle1(title)

        title2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle2)
        if (title2.isNullOrEmpty()) {
            midLine!!.visibility = View.GONE
            titleText2!!.visibility = View.GONE
        } else {
            titleText2!!.text = title2
        }

        hint1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint1)
        hint1?.let {
            searchEdit1!!.hint = hint1
        }

        val maxWidth = toPixels(context, MAX_SINGLE_SEARCH_DP_WIDTH)
        when (type) {
            MultiSearchBar.Type.One, MultiSearchBar.Type.Popup -> searchEdit1!!.maxWidth = maxWidth
            MultiSearchBar.Type.Two -> {
                searchEdit1!!.maxWidth = maxWidth / 2 - 2
                searchEdit2!!.maxWidth = maxWidth / 2 - 2
                searchEdit2!!.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            else -> {
            }
        }

        hint2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint2)
        hint2?.let {
            searchEdit2!!.hint = hint2
        }
        hint3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint3)
        hint3?.let {
            searchEdit3!!.hint = hint3
        }

        popupDrawable1 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable1)
        popupDrawable2 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable2)
        popupDrawable3 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable3)

        popupText1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText1)
        popupText2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText2)
        popupText3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText3)

        typedArray.recycle()
    }

    private fun showKeyboard(editText: EditText) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private val hostActivity: Activity?
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return context
                }
                context = context.baseContext
            }
            return null
        }

    private fun closeSoftKeyboard(focused: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    companion object {

        private const val MAX_SINGLE_SEARCH_DP_WIDTH = 240f
        private val TAG = "MultiSearchBar"

        private fun toPixels(context: Context, dp: Float): Int {
            val scale = context.resources.displayMetrics.density
            // Convert the dps to pixels, based on density scale
            //在转换时加上 0.5f，将该数字四舍五入到最接近的整数
            return (dp * scale + 0.5f).toInt()
        }
    }
}