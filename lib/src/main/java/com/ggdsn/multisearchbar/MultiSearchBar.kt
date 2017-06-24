package com.ggdsn.multisearchbar

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
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

class MultiSearchBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var onModeChangedListener: OnModeChangedListener? = null
    private var onFocusChangeListener1: OnFocusChangeListener? = null
    private var onFocusChangeListener2: OnFocusChangeListener? = null
    private var onFocusChangeListener3: OnFocusChangeListener? = null
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

    interface OnPopupItemClickListener {
        fun onItemClick(item: Int)
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
    private var cancelButtonOnClickListener: OnClickListener? = null

    private val layout = View.inflate(context, R.layout.multi_search_bar, this)
    private val leftButton = layout.findViewById(R.id.multiSearchBarButtonLeft) as AppCompatImageButton
    private val searchButton = layout.findViewById(R.id.multiSearchBarButtonSearch) as AppCompatImageButton
    private val cancelButton = layout.findViewById(R.id.multiSearchBarButtonCancel) as AppCompatButton
    private val titleText1 = layout.findViewById(R.id.multiSearchBarTextViewTitle1) as AppCompatTextView
    private val titleText2 = layout.findViewById(R.id.multiSearchBarTextViewTitle2) as AppCompatTextView
    private val searchEdit1 = layout.findViewById(R.id.multiSearchBarEditTextSearch1) as AppCompatEditText
    private val searchEdit2 = layout.findViewById(R.id.multiSearchBarEditTextSearch2) as AppCompatEditText
    private val searchEdit3 = layout.findViewById(R.id.multiSearchBarEditTextSearch3) as AppCompatEditText
    private val underLine = layout.findViewById(R.id.multiSearchBarLine)
    private val midLine = layout.findViewById(R.id.multiSearchBarMidLine)
    private val midLine2 = layout.findViewById(R.id.multiSearchBarMidLine2)
    private val midLine3 = layout.findViewById(R.id.multiSearchBarMidLine3)
    private val searchDefaultDrawable = searchButton.drawable as TransitionDrawable

    private var title2: String? = null
    private var popupDrawable1: Drawable? = null
    private var popupDrawable2: Drawable? = null
    private var popupDrawable3: Drawable? = null
    private val downArrow = layout.findViewById(R.id.multiSearchBarDownArrow) as AppCompatImageView

    private var popupWindow: PopupWindow? = null
    private var lastChosePopupItem: Int = 0
    private var leftButtonOnClick: OnClickListener? = null

    fun setLeftButtonOnClickListener(leftButtonOnClick: OnClickListener) {
        this.leftButtonOnClick = leftButtonOnClick
    }

    fun setCancelButtonOnClickListener(cancelButtonOnClickListener: OnClickListener) {
        this.cancelButtonOnClickListener = cancelButtonOnClickListener
    }

    fun addTextChangedListener1(watcher: TextWatcher) {
        searchEdit1.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener1(watcher: TextWatcher) {
        searchEdit1.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener2(watcher: TextWatcher) {
        searchEdit2.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener2(watcher: TextWatcher) {
        searchEdit2.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener3(watcher: TextWatcher) {
        searchEdit3.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener3(watcher: TextWatcher) {
        searchEdit3.removeTextChangedListener(watcher)
    }

    fun setOnFocusChangeListener1(l: OnFocusChangeListener) {
        onFocusChangeListener1 = l
    }

    fun setOnFocusChangeListener2(l: OnFocusChangeListener) {
        onFocusChangeListener2 = l
    }

    fun setOnFocusChangeListener3(l: OnFocusChangeListener) {
        onFocusChangeListener3 = l
    }

    fun setTitle1(title: String?) {
        if (title == null) {
            return
        }
        titleText1.text = title
    }

    fun setTitle2(title: String?) {
        if (title == null) {
            return
        }
        title2 = title
        titleText2.text = title2
        if (mode == Mode.Input) {
            return
        }
        midLine.visibility = View.VISIBLE
        titleText2.visibility = View.VISIBLE
    }

    @JvmOverloads fun switchMode(newMode: Mode, withAnimation: Boolean = true) {
        if (mode == newMode) {
            return
        }

        when (newMode) {
            Mode.Normal -> toNormalMode(withAnimation)
            Mode.Input -> toInputMode(withAnimation)
        }
    }

    fun setOnModeChangedListener(listener: OnModeChangedListener) {
        onModeChangedListener = listener
    }

    private var onPopupItemClickListener: OnPopupItemClickListener? = null

    fun setOnPopupItemClickListener(l: OnPopupItemClickListener) {
        onPopupItemClickListener = l
    }

    init {
        setupViews(attrs)
    }

    private fun setupViews(attrs: AttributeSet?) {
        attrs?.let {
            parseXml(attrs)
        }
        searchButton.setOnClickListener(View.OnClickListener { v ->
            if (mode == Mode.Input) {
                if (type == Type.Popup) {
                    popupWindow?.showAsDropDown(searchButton)
                }
                return@OnClickListener
            }
            toInputMode(true)
        })

        cancelButton.setOnClickListener { v ->
            cancelButtonOnClickListener?.onClick(v)
            toNormalMode(true)
        }

        leftButton.setOnClickListener { v ->
            if (leftButtonOnClick == null) {
                val activity = hostActivity
                activity?.onBackPressed()
            } else {
                leftButtonOnClick!!.onClick(v)
            }
        }

        val focusChangeListener = FocusChangeListener()
        searchEdit1.onFocusChangeListener = focusChangeListener
        searchEdit2.onFocusChangeListener = focusChangeListener
        searchEdit3.onFocusChangeListener = focusChangeListener

        if (type == Type.Popup) {
            fun onItemClick(line: Int) {
                setPopupItem(line)
                popupWindow?.dismiss()
                onPopupItemClickListener?.onItemClick(line)
            }

            val view = LayoutInflater.from(context).inflate(R.layout.multi_search_bar_popup, null)

            if (popupText1.isNullOrEmpty()) {
                view.findViewById(R.id.multiSearchBarPopupLine1).visibility = View.GONE
            } else {

                if (popupDrawable1 == null) {
                    view.findViewById(R.id.multiSearchBarPopupDrawable1).visibility = View.GONE
                } else {
                    val imageView1 = view.findViewById(R.id.multiSearchBarPopupDrawable1) as AppCompatImageView
                    imageView1.setImageDrawable(popupDrawable1Black)
                }

                val textView1 = view.findViewById(R.id.multiSearchBarPopupText1) as AppCompatTextView
                textView1.text = popupText1
                view.findViewById(R.id.multiSearchBarPopupLine1).setOnClickListener { onItemClick(0) }
            }

            if (popupText2.isNullOrEmpty()) {
                view.findViewById(R.id.multiSearchBarPopupLine2).visibility = View.GONE
            } else {
                if (popupDrawable2 == null) {
                    view.findViewById(R.id.multiSearchBarPopupDrawable2).visibility = View.GONE
                } else {
                    val imageView2 = view.findViewById(R.id.multiSearchBarPopupDrawable2) as AppCompatImageView
                    imageView2.setImageDrawable(popupDrawable2Black)
                }

                val textView2 = view.findViewById(R.id.multiSearchBarPopupText2) as AppCompatTextView
                textView2.text = popupText2
                view.findViewById(R.id.multiSearchBarPopupLine2).setOnClickListener { onItemClick(1) }
            }

            if (popupText3.isNullOrEmpty()) {
                view.findViewById(R.id.multiSearchBarPopupLine3).visibility = View.GONE
            } else {
                if (popupDrawable3 == null) {
                    view.findViewById(R.id.multiSearchBarPopupDrawable3).visibility = View.GONE
                } else {
                    val imageView3 = view.findViewById(R.id.multiSearchBarPopupDrawable3) as AppCompatImageView
                    imageView3.setImageDrawable(popupDrawable3Black)
                }
                val textView3 = view.findViewById(R.id.multiSearchBarPopupText3) as AppCompatTextView
                textView3.text = popupText3
                view.findViewById(R.id.multiSearchBarPopupLine3).setOnClickListener { onItemClick(2) }
            }

            popupWindow = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow!!.isOutsideTouchable = true
            popupWindow!!.setBackgroundDrawable(ColorDrawable(0x00000000))
        }
    }

    /**
     * 可切换当前选择的popup item，非popup模式无效
     */
    fun setPopupItem(item: Int) {
        if (type != Type.Popup) {
            return
        }

        lastChosePopupItem = item
        when (item) {
            0 -> {
                searchEdit1.hint = hint1
                if (mode == Mode.Input)
                    searchButton.setImageDrawable(popupDrawable1)
            }
            1 -> {
                searchEdit1.hint = hint2
                if (mode == Mode.Input)
                    searchButton.setImageDrawable(popupDrawable2)
            }
            2 -> {
                searchEdit1.hint = hint3
                if (mode == Mode.Input)
                    searchButton.setImageDrawable(popupDrawable3)
            }
        }
    }

    fun getPopupItem(): Int {
        return lastChosePopupItem
    }

    private fun toInputMode(withAnimation: Boolean = true) {
        // TODO: 22/03/2017 添加动画开关
        if (mode == Mode.Input) {
            return
        }

        mode = Mode.Input
        leftButton.visibility = View.INVISIBLE
        cancelButton.visibility = View.VISIBLE
        titleText1.visibility = View.INVISIBLE
        midLine.visibility = View.GONE
        titleText2.visibility = View.GONE
        underLine.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(searchButton, View.TRANSLATION_X, searchButton.paddingLeft - searchButton.x).start()
        val drawable = searchButton.drawable as TransitionDrawable
        drawable.startTransition(100)

        val searchEditAnim = AlphaAnimation(0f, 1f)
        searchEditAnim.duration = 300

        val searchBtnAnim = AlphaAnimation(1f, 0f)
        searchBtnAnim.duration = 300
        searchBtnAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                searchButton.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        fun showKeyboard(editText: EditText) {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        when (type) {
            Type.Popup -> {
                when (lastChosePopupItem) {
                    0 -> searchButton.setImageDrawable(popupDrawable1)
                    1 -> searchButton.setImageDrawable(popupDrawable2)
                    2 -> searchButton.setImageDrawable(popupDrawable3)
                }
                downArrow.visibility = View.VISIBLE

                midLine2.visibility = View.GONE
                midLine3.visibility = View.GONE
                searchEdit2.visibility = View.GONE
                searchEdit3.visibility = View.GONE

                searchEdit1.startAnimation(searchEditAnim)
                searchEdit1.visibility = View.VISIBLE

                permitFocus = true
                searchEdit1.requestFocus()
                showKeyboard(searchEdit1)
            }
            Type.One -> {
                midLine2.visibility = View.GONE
                midLine3.visibility = View.GONE
                searchEdit2.visibility = View.GONE
                searchEdit3.visibility = View.GONE
                searchEdit1.startAnimation(searchEditAnim)
                searchEdit1.visibility = View.VISIBLE
                permitFocus = true
                searchEdit1.requestFocus()
                showKeyboard(searchEdit1)
            }
            Type.Two -> {
                searchButton.startAnimation(searchBtnAnim)
                leftButton.visibility = View.GONE

                midLine3.visibility = View.GONE
                searchEdit3.visibility = View.GONE

                searchEdit1.startAnimation(searchEditAnim)
                searchEdit1.visibility = View.VISIBLE

                midLine2.startAnimation(searchEditAnim)
                midLine2.visibility = View.VISIBLE

                searchEdit2.visibility = View.VISIBLE
                searchEdit2.startAnimation(searchEditAnim)
                // FIXME: 26/05/2017 改变最小长度，免得太短太难点
                permitFocus = true
                searchEdit1.requestFocus()
                showKeyboard(searchEdit1)
            }
            Type.Three -> {
                searchButton.startAnimation(searchBtnAnim)
                leftButton.visibility = View.GONE

                searchEdit1.startAnimation(searchEditAnim)
                searchEdit1.visibility = View.VISIBLE

                midLine2.visibility = View.VISIBLE
                midLine2.startAnimation(searchEditAnim)
                midLine3.visibility = View.VISIBLE
                midLine3.startAnimation(searchEditAnim)
                searchEdit2.visibility = View.VISIBLE
                searchEdit2.startAnimation(searchEditAnim)
                searchEdit3.visibility = View.VISIBLE
                searchEdit3.startAnimation(searchEditAnim)

                permitFocus = true
                searchEdit1.requestFocus()
                showKeyboard(searchEdit1)
            }
        }
        onModeChangedListener?.onNewMode(mode)
    }

    private fun toNormalMode(withAnimation: Boolean = true) {
        if (mode == Mode.Normal) {
            return
        }
        mode = Mode.Normal

        val focus = layout.findFocus()
        focus?.clearFocus()
        permitFocus = false

        leftButton.visibility = View.VISIBLE
        downArrow.visibility = View.GONE
        underLine.visibility = View.GONE
        cancelButton.visibility = View.INVISIBLE
        titleText1.visibility = View.VISIBLE
        if (!TextUtils.isEmpty(title2)) {
            midLine.visibility = View.VISIBLE
            titleText2.visibility = View.VISIBLE
        }
        searchEdit1.visibility = View.INVISIBLE
        searchEdit2.visibility = View.GONE
        searchEdit3.visibility = View.GONE
        midLine2.visibility = View.GONE
        midLine3.visibility = View.GONE

        when (type) {
            Type.Two, Type.Three -> {
                val searchBtnAnim = AlphaAnimation(0f, 1f)
                searchBtnAnim.duration = 300
                searchButton.animation = searchBtnAnim
                ObjectAnimator.ofFloat<View>(searchButton, View.TRANSLATION_X, 0f).start()
                val drawable = searchButton.drawable as TransitionDrawable
                drawable.reverseTransition(100)
            }
            Type.One, Type.Popup -> {
                searchButton.setImageDrawable(searchDefaultDrawable)
                ObjectAnimator.ofFloat<View>(searchButton, View.TRANSLATION_X, 0f).start()
                searchDefaultDrawable.reverseTransition(100)
            }
        }
        searchButton.visibility = View.VISIBLE

        fun closeSoftKeyboard(focused: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(focused.windowToken, 0)
        }
        closeSoftKeyboard(layout)

        onModeChangedListener?.onNewMode(mode)
    }

    private inner class FocusChangeListener : OnFocusChangeListener {

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

    private var popupDrawable1Black: Drawable? = null

    private var popupDrawable2Black: Drawable? = null

    private var popupDrawable3Black: Drawable? = null

    private fun parseXml(attrs: AttributeSet) {
        fun toPixels(context: Context, dp: Float): Int {
            val scale = context.resources.displayMetrics.density
            // Convert the dps to pixels, based on density scale
            //在转换时加上 0.5f，将该数字四舍五入到最接近的整数
            return (dp * scale + 0.5f).toInt()
        }

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiSearchBar)

        type = Type.values()[typedArray.getInt(R.styleable.MultiSearchBar_multiSearchBarType, Type.One.ordinal)]

        val leftDrawableId = typedArray.getResourceId(R.styleable.MultiSearchBar_multiSearchBarLeftSrc, -1)
        if (leftDrawableId != -1) {
            leftButton.setImageResource(leftDrawableId)
        }

        val title = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle1)
        setTitle1(title)

        title2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle2)
        if (title2.isNullOrEmpty()) {
            midLine.visibility = View.GONE
            titleText2.visibility = View.GONE
        } else {
            titleText2.text = title2
        }

        hint1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint1)
        hint1?.let {
            searchEdit1.hint = hint1
        }

        val maxWidth = toPixels(context, MAX_SINGLE_SEARCH_DP_WIDTH)
        when (type) {
            Type.One, Type.Popup -> searchEdit1.maxWidth = maxWidth
            Type.Two -> {
                searchEdit1.maxWidth = maxWidth / 2 - 2
                searchEdit2.maxWidth = maxWidth / 2 - 2
                searchEdit2.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            else -> {
            }
        }

        hint2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint2)
        hint2?.let {
            searchEdit2.hint = hint2
        }
        hint3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint3)
        hint3?.let {
            searchEdit3.hint = hint3
        }

        val white = LightingColorFilter(Color.WHITE, Color.WHITE)
        val black = LightingColorFilter(Color.BLACK, Color.BLACK)
        popupDrawable1 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable1)
        popupDrawable1?.colorFilter = white
        popupDrawable1Black = popupDrawable1?.constantState?.newDrawable()?.mutate()
        popupDrawable1Black?.colorFilter = black

        popupDrawable2 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable2)
        popupDrawable2?.colorFilter = white
        popupDrawable2Black = popupDrawable2?.constantState?.newDrawable()?.mutate()
        popupDrawable2Black?.colorFilter = black

        popupDrawable3 = typedArray.getDrawable(R.styleable.MultiSearchBar_multiSearchBarPopupDrawable3)
        popupDrawable3?.colorFilter = white
        popupDrawable3Black = popupDrawable3?.constantState?.newDrawable()?.mutate()
        popupDrawable3Black?.colorFilter = black

        popupText1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText1)
        popupText2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText2)
        popupText3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarPopupText3)

        typedArray.recycle()
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

    companion object {
        private const val MAX_SINGLE_SEARCH_DP_WIDTH = 240f
    }
}