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
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
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
import kotlinx.android.synthetic.main.multi_search_bar.view.*

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
        /**
         * @param[popItemIndex] popup当前的位置，如果是关闭状态，默认值为-1
         */
        fun onNewSearchBarMode(newMode: Mode, popItemIndex: Int = -1, fromClick: Boolean)
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
    /**
     * 低版本不兼容transition套vector
     */
    private val searchDefaultDrawable = TransitionDrawable(arrayOf(AppCompatResources.getDrawable(context, R.drawable.multi_search_bar_ic_search),
            AppCompatResources.getDrawable(context, R.drawable.multi_search_bar_ic_search_gold)))

    private var title2: String? = null
    private var popupDrawable1: Drawable? = null
    private var popupDrawable2: Drawable? = null
    private var popupDrawable3: Drawable? = null

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
        multiSearchBarEditTextSearch1.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener1(watcher: TextWatcher) {
        multiSearchBarEditTextSearch1.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener2(watcher: TextWatcher) {
        multiSearchBarEditTextSearch2.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener2(watcher: TextWatcher) {
        multiSearchBarEditTextSearch2.removeTextChangedListener(watcher)
    }

    fun addTextChangedListener3(watcher: TextWatcher) {
        multiSearchBarEditTextSearch3.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener3(watcher: TextWatcher) {
        multiSearchBarEditTextSearch3.removeTextChangedListener(watcher)
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
        if (title == null) return
        multiSearchBarTextViewTitle1.text = title
    }

    fun setTitle2(title: String?) {
        if (title == null) return
        title2 = title
        multiSearchBarTextViewTitle2.text = title2
        if (mode == Mode.Input) return
        multiSearchBarMidLine.visibility = View.VISIBLE
        multiSearchBarTextViewTitle2.visibility = View.VISIBLE
    }

    @JvmOverloads
    fun switchMode(newMode: Mode, withAnimation: Boolean = true) {
        if (mode == newMode) return

        when (newMode) {
            Mode.Normal -> toNormalMode(withAnimation, false)
            Mode.Input -> toInputMode(withAnimation, false)
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
        multiSearchBarButtonSearch.setImageDrawable(searchDefaultDrawable)
        multiSearchBarButtonSearch.setOnClickListener(View.OnClickListener { v ->
            if (mode == Mode.Input) {
                if (type == Type.Popup) popupWindow?.showAsDropDown(multiSearchBarButtonSearch)
                return@OnClickListener
            }
            toInputMode(true, true)
        })

        multiSearchBarButtonCancel.setOnClickListener { v ->
            cancelButtonOnClickListener?.onClick(v)
            toNormalMode(true, true)
        }

        multiSearchBarButtonLeft.setOnClickListener { v ->
            if (leftButtonOnClick == null) {
                val activity = hostActivity
                activity?.onBackPressed()
            } else {
                leftButtonOnClick!!.onClick(v)
            }
        }

        val focusChangeListener = FocusChangeListener()
        multiSearchBarEditTextSearch1.onFocusChangeListener = focusChangeListener
        multiSearchBarEditTextSearch2.onFocusChangeListener = focusChangeListener
        multiSearchBarEditTextSearch3.onFocusChangeListener = focusChangeListener

        if (type == Type.Popup) {
            fun onItemClick(line: Int) {
                setPopupItem(line)
                popupWindow?.dismiss()
                onPopupItemClickListener?.onItemClick(line)
            }

            val view = LayoutInflater.from(context).inflate(R.layout.multi_search_bar_popup, null)

            if (popupText1.isNullOrEmpty()) {
                view.findViewById<View>(R.id.multiSearchBarPopupLine1).visibility = View.GONE
            } else {

                if (popupDrawable1 == null) {
                    view.findViewById<View>(R.id.multiSearchBarPopupDrawable1).visibility = View.GONE
                } else {
                    val imageView1 = view.findViewById<AppCompatImageView>(R.id.multiSearchBarPopupDrawable1) as AppCompatImageView
                    imageView1.setImageDrawable(popupDrawable1Black)
                }

                val textView1 = view.findViewById<AppCompatTextView>(R.id.multiSearchBarPopupText1) as AppCompatTextView
                textView1.text = popupText1
                view.findViewById<View>(R.id.multiSearchBarPopupLine1).setOnClickListener { onItemClick(0) }
            }

            if (popupText2.isNullOrEmpty()) {
                view.findViewById<View>(R.id.multiSearchBarPopupLine2).visibility = View.GONE
            } else {
                if (popupDrawable2 == null) {
                    view.findViewById<View>(R.id.multiSearchBarPopupDrawable2).visibility = View.GONE
                } else {
                    val imageView2 = view.findViewById<AppCompatImageView>(R.id.multiSearchBarPopupDrawable2) as AppCompatImageView
                    imageView2.setImageDrawable(popupDrawable2Black)
                }

                val textView2 = view.findViewById<AppCompatTextView>(R.id.multiSearchBarPopupText2) as AppCompatTextView
                textView2.text = popupText2
                view.findViewById<View>(R.id.multiSearchBarPopupLine2).setOnClickListener { onItemClick(1) }
            }

            if (popupText3.isNullOrEmpty()) {
                view.findViewById<View>(R.id.multiSearchBarPopupLine3).visibility = View.GONE
            } else {
                if (popupDrawable3 == null) {
                    view.findViewById<View>(R.id.multiSearchBarPopupDrawable3).visibility = View.GONE
                } else {
                    val imageView3 = view.findViewById<AppCompatImageView>(R.id.multiSearchBarPopupDrawable3) as AppCompatImageView
                    imageView3.setImageDrawable(popupDrawable3Black)
                }
                val textView3 = view.findViewById<AppCompatTextView>(R.id.multiSearchBarPopupText3) as AppCompatTextView
                textView3.text = popupText3
                view.findViewById<View>(R.id.multiSearchBarPopupLine3).setOnClickListener { onItemClick(2) }
            }

            val popupWindow = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow.isOutsideTouchable = true
            popupWindow.setBackgroundDrawable(ColorDrawable(0x00000000))
            this.popupWindow = popupWindow
        }
    }

    fun clearAllInput() {
        multiSearchBarEditTextSearch1.setText("")
        multiSearchBarEditTextSearch2.setText("")
        multiSearchBarEditTextSearch3.setText("")
    }

    /**
     * 可切换当前选择的popup item，非popup模式无效
     */
    fun setPopupItem(item: Int) {
        if (type != Type.Popup) return

        lastChosePopupItem = item
        when (item) {
            0 -> {
                multiSearchBarEditTextSearch1.hint = hint1
                if (mode == Mode.Input)
                    multiSearchBarButtonSearch.setImageDrawable(popupDrawable1)
            }
            1 -> {
                multiSearchBarEditTextSearch1.hint = hint2
                if (mode == Mode.Input)
                    multiSearchBarButtonSearch.setImageDrawable(popupDrawable2)
            }
            2 -> {
                multiSearchBarEditTextSearch1.hint = hint3
                if (mode == Mode.Input)
                    multiSearchBarButtonSearch.setImageDrawable(popupDrawable3)
            }
        }
    }

    fun getPopupItem(): Int = lastChosePopupItem

    private fun toInputMode(withAnimation: Boolean = true, fromClick: Boolean) {
        // TODO: 22/03/2017 添加动画开关
        if (mode == Mode.Input) {
            return
        }

        mode = Mode.Input
        multiSearchBarButtonLeft.visibility = View.INVISIBLE
        multiSearchBarButtonCancel.visibility = View.VISIBLE
        multiSearchBarTextViewTitle1.visibility = View.INVISIBLE
        multiSearchBarMidLine.visibility = View.GONE
        multiSearchBarTextViewTitle2.visibility = View.GONE
        multiSearchBarLine.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(multiSearchBarButtonSearch, View.TRANSLATION_X, multiSearchBarButtonSearch.paddingLeft - multiSearchBarButtonSearch.x).start()
        val drawable = multiSearchBarButtonSearch.drawable as TransitionDrawable
        drawable.startTransition(100)

        val searchEditAnim = AlphaAnimation(0f, 1f)
        searchEditAnim.duration = 300

        val searchBtnAnim = AlphaAnimation(1f, 0f)
        searchBtnAnim.duration = 300
        searchBtnAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                multiSearchBarButtonSearch.visibility = View.GONE
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
                    0 -> multiSearchBarButtonSearch.setImageDrawable(popupDrawable1)
                    1 -> multiSearchBarButtonSearch.setImageDrawable(popupDrawable2)
                    2 -> multiSearchBarButtonSearch.setImageDrawable(popupDrawable3)
                }
                multiSearchBarDownArrow.visibility = View.VISIBLE

                multiSearchBarMidLine2.visibility = View.GONE
                multiSearchBarMidLine3.visibility = View.GONE
                multiSearchBarEditTextSearch2.visibility = View.GONE
                multiSearchBarEditTextSearch3.visibility = View.GONE

                multiSearchBarEditTextSearch1.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch1.visibility = View.VISIBLE

                permitFocus = true
                multiSearchBarEditTextSearch1.requestFocus()
                showKeyboard(multiSearchBarEditTextSearch1)
            }
            Type.One -> {
                multiSearchBarMidLine2.visibility = View.GONE
                multiSearchBarMidLine3.visibility = View.GONE
                multiSearchBarEditTextSearch2.visibility = View.GONE
                multiSearchBarEditTextSearch3.visibility = View.GONE
                multiSearchBarEditTextSearch1.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch1.visibility = View.VISIBLE
                permitFocus = true
                multiSearchBarEditTextSearch1.requestFocus()
                showKeyboard(multiSearchBarEditTextSearch1)
            }
            Type.Two -> {
                multiSearchBarButtonSearch.startAnimation(searchBtnAnim)
                multiSearchBarButtonLeft.visibility = View.GONE

                multiSearchBarMidLine3.visibility = View.GONE
                multiSearchBarEditTextSearch3.visibility = View.GONE

                multiSearchBarEditTextSearch1.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch1.visibility = View.VISIBLE

                multiSearchBarMidLine2.startAnimation(searchEditAnim)
                multiSearchBarMidLine2.visibility = View.VISIBLE

                multiSearchBarEditTextSearch2.visibility = View.VISIBLE
                multiSearchBarEditTextSearch2.startAnimation(searchEditAnim)
                // FIXME: 26/05/2017 改变最小长度，免得太短太难点
                permitFocus = true
                multiSearchBarEditTextSearch1.requestFocus()
                showKeyboard(multiSearchBarEditTextSearch1)
            }
            Type.Three -> {
                multiSearchBarButtonSearch.startAnimation(searchBtnAnim)
                multiSearchBarButtonLeft.visibility = View.GONE

                multiSearchBarEditTextSearch1.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch1.visibility = View.VISIBLE

                multiSearchBarMidLine2.visibility = View.VISIBLE
                multiSearchBarMidLine2.startAnimation(searchEditAnim)
                multiSearchBarMidLine3.visibility = View.VISIBLE
                multiSearchBarMidLine3.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch2.visibility = View.VISIBLE
                multiSearchBarEditTextSearch2.startAnimation(searchEditAnim)
                multiSearchBarEditTextSearch3.visibility = View.VISIBLE
                multiSearchBarEditTextSearch3.startAnimation(searchEditAnim)

                permitFocus = true
                multiSearchBarEditTextSearch1.requestFocus()
                showKeyboard(multiSearchBarEditTextSearch1)
            }
        }
        onModeChangedListener?.onNewSearchBarMode(mode, lastChosePopupItem, fromClick)
    }

    private fun toNormalMode(withAnimation: Boolean = true, fromClick: Boolean) {
        if (mode == Mode.Normal) {
            return
        }
        mode = Mode.Normal

        val focus = layout.findFocus()
        focus?.clearFocus()
        permitFocus = false

        multiSearchBarButtonLeft.visibility = View.VISIBLE
        multiSearchBarDownArrow.visibility = View.GONE
        multiSearchBarLine.visibility = View.GONE
        multiSearchBarButtonCancel.visibility = View.INVISIBLE
        multiSearchBarTextViewTitle1.visibility = View.VISIBLE
        if (!TextUtils.isEmpty(title2)) {
            multiSearchBarMidLine.visibility = View.VISIBLE
            multiSearchBarTextViewTitle2.visibility = View.VISIBLE
        }
        multiSearchBarEditTextSearch1.visibility = View.INVISIBLE
        multiSearchBarEditTextSearch2.visibility = View.GONE
        multiSearchBarEditTextSearch3.visibility = View.GONE
        multiSearchBarMidLine2.visibility = View.GONE
        multiSearchBarMidLine3.visibility = View.GONE

        when (type) {
            Type.Two, Type.Three -> {
                val searchBtnAnim = AlphaAnimation(0f, 1f)
                searchBtnAnim.duration = 300
                multiSearchBarButtonSearch.animation = searchBtnAnim
                ObjectAnimator.ofFloat<View>(multiSearchBarButtonSearch, View.TRANSLATION_X, 0f).start()
                val drawable = multiSearchBarButtonSearch.drawable as TransitionDrawable
                drawable.reverseTransition(100)
            }
            Type.One, Type.Popup -> {
                multiSearchBarButtonSearch.setImageDrawable(searchDefaultDrawable)
                ObjectAnimator.ofFloat<View>(multiSearchBarButtonSearch, View.TRANSLATION_X, 0f).start()
                searchDefaultDrawable.reverseTransition(100)
            }
        }
        multiSearchBarButtonSearch.visibility = View.VISIBLE

        fun closeSoftKeyboard(focused: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(focused.windowToken, 0)
        }
        closeSoftKeyboard(layout)

        onModeChangedListener?.onNewSearchBarMode(mode, -1, fromClick)
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
            multiSearchBarButtonLeft.setImageResource(leftDrawableId)
        }

        val title = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle1)
        setTitle1(title)

        title2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarTitle2)
        if (title2.isNullOrEmpty()) {
            multiSearchBarMidLine.visibility = View.GONE
            multiSearchBarTextViewTitle2.visibility = View.GONE
        } else {
            multiSearchBarTextViewTitle2.text = title2
        }

        hint1 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint1)
        hint1?.let {
            multiSearchBarEditTextSearch1.hint = hint1
        }

        val maxWidth = toPixels(context, MAX_SINGLE_SEARCH_DP_WIDTH)
        when (type) {
            Type.One, Type.Popup -> multiSearchBarEditTextSearch1.maxWidth = maxWidth
            Type.Two -> {
                multiSearchBarEditTextSearch1.maxWidth = maxWidth / 2 - 2
                multiSearchBarEditTextSearch2.maxWidth = maxWidth / 2 - 2
                multiSearchBarEditTextSearch2.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            else -> {
            }
        }

        hint2 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint2)
        hint2?.let {
            multiSearchBarEditTextSearch2.hint = hint2
        }
        hint3 = typedArray.getString(R.styleable.MultiSearchBar_multiSearchBarHint3)
        hint3?.let {
            multiSearchBarEditTextSearch3.hint = hint3
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