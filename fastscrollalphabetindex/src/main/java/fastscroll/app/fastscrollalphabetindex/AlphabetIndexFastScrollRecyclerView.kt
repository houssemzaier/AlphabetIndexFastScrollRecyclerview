package fastscroll.app.fastscrollalphabetindex

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val WHAT_FADE_PREVIEW = 1

class AlphabetIndexFastScrollRecyclerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    private val density: Float = context.resources.displayMetrics.density
    private val scaledDensity: Float = context.resources.displayMetrics.scaledDensity

    //region IndexBarVisibility and PreviewVisibility (NO XML ATTRS CONFIGURATION)
    var isIndexBarVisible = true
        set(value) {
            field = value
            invalidate()
        }

    var isPreviewVisible = true
        set(value) {
            field = value
            invalidate()
        }
    //endregion

    //region IndexBarLayout Width, Margin, radius, backgroundColor, BackgroundTransparency
    var indexBarWidth = 20f
        set(value) {
            field = value * density
            invalidate()
        }

    var indexBarMargin = 20f
        set(value) {
            field = value * density
            invalidate()
        }

    var indexBarCornerRadius = 5
        set(value) {
            field = (value * density).toInt()
            invalidate()
        }

    @JvmField
    @ColorInt
    var indexBarBackgroundColor = Color.BLACK

    /*** @param color The color for the index bar*/
    fun setIndexBarBackgroundColor(color: String?) {
        val colorValue = Color.parseColor(color)
        indexBarBackgroundColor = colorValue
        invalidate()
    }

    /*** @param color The color for the index bar*/
    fun setIndexBarBackgroundColor(@ColorRes color: Int) {
        val colorValue = ContextCompat.getColor(context, color)
        indexBarBackgroundColor = colorValue
        invalidate()
    }

    var indexBarBackgroundTransparency = 0.6F
        set(value) {
            field = value
            indexBarBackgroundAlpha = value.toInt()
        }
    private var indexBarBackgroundAlpha = convertTransparentValueToBackgroundAlpha(indexBarBackgroundTransparency)
        set(value) {
            field = convertTransparentValueToBackgroundAlpha(value.toFloat())
        }

    private fun convertTransparentValueToBackgroundAlpha(value: Float): Int {
        return (255 * value).toInt()
    }
    //endregion

    //region IndexTextSize, highlightColor,highlightTextColor
    var indexBarTextSize = 12
        set(value) {
            field = value
            invalidate()//maybe not
        }

    @JvmField
    @ColorInt
    var indexBarTextColor = Color.WHITE

    fun setIndexBarTextColor(color: String?) {
        val colorValue = Color.parseColor(color)
        indexBarTextColor = colorValue
    }

    fun setIndexBarTextColor(@ColorRes color: Int) {
        val colorValue = ContextCompat.getColor(context, color)
        indexBarTextColor = colorValue
    }

    @JvmField
    @ColorInt
    var indexBarHighlightTextColor = Color.BLACK

    fun setIndexBarHighlightTextColor(color: String?) {
        val colorValue = Color.parseColor(color)
        indexBarHighlightTextColor = colorValue
    }

    var isIndexBarHighlightTextVisibility = false

    fun setIndexBarHighlightTextColor(@ColorRes color: Int) {
        val colorValue = ContextCompat.getColor(context, color)
        indexBarHighlightTextColor = colorValue
    }

    //endregion

    //region PreviewPadding (NO XML ATTRS CONFIGURATION)
    var previewSectionPadding = 5F
        set(value) {
            field = value * density
        }
    //endregion PreviewPadding

    //region typeface
    private var typeface: Typeface? = null

    //endregion

    //paints
    private val indexBarPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private val previewPaint: Paint = Paint().apply {
        color = Color.BLACK
        alpha = 96
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, Color.argb(64, 0, 0, 0))
    }
    private val indexBarTextPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private val previewTextPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = 50 * scaledDensity
    }

    private var calculatedWidth = 0
    private var calculatedHeight = 0

    private lateinit var indexBarLayoutRect: RectF

    // all sections
    private lateinit var sections: Array<String>

    //current selected sectionIndex
    private var currentSectionIndex = -1

    //when touch occurs
    private var isIndexing = false

    private val adapterObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            Log.i("INDEX_BAR_PAINT", "AdapterDataObserver#onChanged ")
            sections = adapter?.sections as? Array<String> ?: emptyArray()
        }
    }

    //Section Helper class
    var adapter: SectionIndexerAdapter<*>? = null
        set(value) {
            field = value
            super.setAdapter(adapter)
            if (field != null) {
                adapter?.registerAdapterDataObserver(adapterObserver)
                sections = adapter?.sections as? Array<String> ?: emptyArray()
            }
        }

    @SuppressLint("HandlerLeak")
    private val invalidationHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == WHAT_FADE_PREVIEW) {
                currentSectionIndex = -1
                invalidate()
            }
        }
    }

    private var gestureDetector = GestureDetector(context, SimpleOnGestureListener())

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AlphabetIndexFastScrollRecyclerView, 0, 0)
            try {
                //IndexBarLayout indexBarMargin indexBarCornerRadius
                indexBarWidth = typedArray.getFloat(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarWidth, indexBarWidth)
                indexBarMargin = typedArray.getFloat(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarMargin, indexBarMargin)
                indexBarCornerRadius = typedArray.getInt(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarCornerRadius, indexBarCornerRadius)

                //IndexBarBackground IndexBarBackground
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarBackgroundColor)) {
                    indexBarBackgroundColor = Color.parseColor(typedArray.getString(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarBackgroundColor))
                }
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarColorBackgroundRes)) {
                    indexBarBackgroundColor = typedArray.getColor(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarColorBackgroundRes, indexBarBackgroundColor)
                }
                indexBarBackgroundTransparency = typedArray.getFloat(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTransparentValue, indexBarBackgroundTransparency)

                //IndexBar IndexTextSize, highlightColor,highlightTextColor
                indexBarTextSize = typedArray.getInt(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTextSize, indexBarTextSize)
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTextColor)) {
                    indexBarTextColor = Color.parseColor(typedArray.getString(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTextColor))
                }
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTextColorRes)) {
                    indexBarTextColor = typedArray.getColor(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarTextColorRes, indexBarTextColor)
                }
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarHighlightTextColor)) {
                    indexBarHighlightTextColor = Color.parseColor(typedArray.getString(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarHighlightTextColor))
                }
                if (typedArray.hasValue(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarHighlightTextColorRes)) {
                    indexBarHighlightTextColor = typedArray.getColor(R.styleable.AlphabetIndexFastScrollRecyclerView_setIndexBarHighlightTextColor, indexBarHighlightTextColor)
                }

                //PreviewPadding
                previewSectionPadding = typedArray.getInt(R.styleable.AlphabetIndexFastScrollRecyclerView_setPreviewSectionPadding, previewSectionPadding.toInt()).toFloat()
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun onSizeChanged(currentWidth: Int, currentHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        calculatedWidth = currentWidth
        calculatedHeight = currentHeight

        indexBarLayoutRect = RectF().apply {
            left = currentWidth - indexBarMargin - indexBarWidth
            top = indexBarMargin
            right = currentWidth - indexBarMargin
            bottom = currentHeight - indexBarMargin
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (!isIndexBarVisible) return
        drawIndexBarLayout(canvas)
        drawSectionsAndPreviewSection(canvas)
    }

    private fun drawIndexBarLayout(canvas: Canvas) {
        indexBarPaint.color = indexBarBackgroundColor
        indexBarPaint.alpha = indexBarBackgroundAlpha
        canvas.drawRoundRect(indexBarLayoutRect, indexBarCornerRadius.toFloat(), indexBarCornerRadius.toFloat(), indexBarPaint)
    }

    private fun drawSectionsAndPreviewSection(canvas: Canvas) {
        if (sections.isEmpty()) return
        if (shouldShowPreviewSection()) {
            drawPreviewSection(canvas)
            hidePreviewSectionAndHighlightingAfter(800)
        }
        drawSections(canvas)
    }

    private fun drawSections(canvas: Canvas) {
        Log.i("INDEX_BAR_PAINT", "drawSections()")
        indexBarTextPaint.color = indexBarTextColor
        indexBarTextPaint.textSize = indexBarTextSize * scaledDensity
        indexBarTextPaint.typeface = typeface
        val sectionHeight = (indexBarLayoutRect.height() - 2 * indexBarMargin) / sections.size
        val paddingTop = (sectionHeight - (indexBarTextPaint.descent() - indexBarTextPaint.ascent())) / 2
        for (i in sections.indices) {
            if (isIndexBarHighlightTextVisibility) {
                if (i == currentSectionIndex) {
                    indexBarTextPaint.typeface = Typeface.create(typeface, Typeface.BOLD)
                    indexBarTextPaint.textSize = (indexBarTextSize + 3) * scaledDensity
                    indexBarTextPaint.color = indexBarHighlightTextColor
                } else {
                    indexBarTextPaint.typeface = typeface
                    indexBarTextPaint.textSize = indexBarTextSize * scaledDensity
                    indexBarTextPaint.color = indexBarTextColor
                }
                val paddingLeft = (indexBarWidth - indexBarTextPaint.measureText(sections[i])) / 2
                canvas.drawText(sections[i], indexBarLayoutRect.left + paddingLeft
                        , indexBarLayoutRect.top + indexBarMargin + sectionHeight * i + paddingTop - indexBarTextPaint.ascent(), indexBarTextPaint)
            } else {
                val paddingLeft = (indexBarWidth - indexBarTextPaint.measureText(sections[i])) / 2
                canvas.drawText(sections[i], indexBarLayoutRect.left + paddingLeft
                        , indexBarLayoutRect.top + indexBarMargin + sectionHeight * i + paddingTop - indexBarTextPaint.ascent(), indexBarTextPaint)
            }
        }
    }

    private fun shouldShowPreviewSection(): Boolean {
        return (isPreviewVisible && currentSectionIndex != -1 && sections[currentSectionIndex] != "").also {
            Log.i("INDEX_BAR_PAINT", "***isPreviewVisible $isPreviewVisible ")
            Log.i("INDEX_BAR_PAINT", "***currentSectionIndex $currentSectionIndex ")
            if (currentSectionIndex != -1)
                Log.i("INDEX_BAR_PAINT", "***isCurrentSectionEmpty ${sections[currentSectionIndex] != ""}")
        }
    }

    private fun drawPreviewSection(canvas: Canvas) {
        previewTextPaint.typeface = typeface
        val previewTextWidth = previewTextPaint.measureText(sections[currentSectionIndex])
        val previewSize = 2 * previewSectionPadding + previewTextPaint.descent() - previewTextPaint.ascent()
        val previewRect = RectF((calculatedWidth - previewSize) / 2,
                (calculatedHeight - previewSize) / 2,
                (calculatedWidth - previewSize) / 2 + previewSize,
                (calculatedHeight - previewSize) / 2 + previewSize)

        canvas.drawRoundRect(previewRect, 5 * density, 5 * density, previewPaint)
        canvas.drawText(sections[currentSectionIndex],
                previewRect.left + (previewSize - previewTextWidth) / 2 - 1,
                previewRect.top + previewSectionPadding - previewTextPaint.ascent() + 1, previewTextPaint)
    }

    private fun hidePreviewSectionAndHighlightingAfter(delay: Long) {
        invalidationHandler.removeMessages(WHAT_FADE_PREVIEW)
        invalidationHandler.sendEmptyMessageAtTime(WHAT_FADE_PREVIEW, SystemClock.uptimeMillis() + delay)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val shouldForwardEventToOnTouchEvent = isIndexBarVisible && indexBarLayoutContains(event.x, event.y)
        return if (shouldForwardEventToOnTouchEvent) {
            true
        } else {
            //forward it to the children
            super.onInterceptTouchEvent(event)
        }
    }

    private fun indexBarLayoutContains(x: Float, y: Float): Boolean {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        // for a ux reason, the finger would be on the right of the indexBarLayout, so not just a classic return indexBarRect.contains(x, y)
        return (x >= indexBarLayoutRect.left &&
                y in indexBarLayoutRect.top..indexBarLayoutRect.bottom)
                .also {
                    Log.i("INDEX_BAR_PAINT", "touch in the indexBarLayout? $it")
                }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i("INDEX_BAR_PAINT", "OnTouchEvent")
        if (isIndexBarVisible) {
            if (onTouchEvent1(event)) {
                return true
            }
            //gestureDetector.onTouchEvent(event) todo: why should I use gestureDetector ?
        }
        return super.onTouchEvent(event)
    }

    private fun onTouchEvent1(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->
                if (indexBarLayoutContains(ev.x, ev.y)) {
                    isIndexing = true
                    currentSectionIndex = getCurrentSectionByPoint(ev.y)
                    scrollRecyclerToPosition()
                    return true
                }
            MotionEvent.ACTION_MOVE -> if (isIndexing) {
                if (indexBarLayoutContains(ev.x, ev.y)) {
                    currentSectionIndex = getCurrentSectionByPoint(ev.y)
                    scrollRecyclerToPosition()
                }
                return true
            }
            MotionEvent.ACTION_UP -> if (isIndexing) {
                isIndexing = false
                //currentSectionIndex = -1
                Log.i("INDEX_BAR_PAINT", "-RESET currentSectionIndex ")
            }
        }
        return false
    }

    private fun getCurrentSectionByPoint(y: Float): Int {
        val isPointOnTopOfIndexBarLayout = y < indexBarLayoutRect.top + indexBarMargin
        val isPointOnBottomOfIndexBarLayout = y >= indexBarLayoutRect.top + indexBarLayoutRect.height() - indexBarMargin
        return when {
            sections.isEmpty() || isPointOnTopOfIndexBarLayout -> 0
            isPointOnBottomOfIndexBarLayout -> sections.lastIndex
            else -> {
                val yInsideIndexBarLayout = y - indexBarLayoutRect.top - indexBarMargin
                val indexBarLayoutWithoutMargin = indexBarLayoutRect.height() - 2 * indexBarMargin
                val sectionIndex = (yInsideIndexBarLayout / (indexBarLayoutWithoutMargin / sections.size)).toInt()
                Log.i("INDEX_BAR_PAINT", "sectionIndex $sectionIndex")
                sectionIndex
            }
        }.also {
            invalidate()
        }
    }

    private fun scrollRecyclerToPosition() {
        Log.i("INDEX_BAR_PAINT", "scrollToPosition() currentSectionIndex $currentSectionIndex")
        Log.i("INDEX_BAR_PAINT", "scrollToPosition() position ${adapter?.getPositionForSection(currentSectionIndex)}")

        val position = adapter?.getPositionForSection(currentSectionIndex) ?: return
        layoutManager?.let {
            if (it is LinearLayoutManager) {
                it.scrollToPositionWithOffset(position, 0)
            } else {
                it.scrollToPosition(position)
            }
        }
    }
}
