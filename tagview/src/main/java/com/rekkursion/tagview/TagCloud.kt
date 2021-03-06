package com.rekkursion.tagview

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout

class TagCloud(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {
    // if this tag-cloud is an indicator or not, default: false
    private var mIsIndicator: Boolean = false
    var isIndicator
        get() = mIsIndicator
        set(value) {
            mIsIndicator = value
            if (mIsIndicator) {
                mFblTagsContainer.children
                    .forEach { (it as? TagView)?.setCloseImageButtonVisibility(View.GONE) }
                mImgbtnAddTag.visibility = View.GONE
            }
            else {
                mFblTagsContainer.children
                    .forEach { (it as? TagView)?.setCloseImageButtonVisibility(View.VISIBLE) }
                mImgbtnAddTag.visibility = View.VISIBLE
            }
        }

    // for placing all tag-views
    private val mFblTagsContainer: FlexboxLayout

    // for adding a new tag
    private val mImgbtnAddTag: ImageButton

    // for storing the already-exists tag-views
    private val mTagStringsHashMap: HashMap<String, TagView> = hashMapOf()

    // the listener when a new tag's string has already added before
    private var mOnTagStringConflictListener: OnTagStringConflictListener? = null

    // the listener when a certain tag-view is removed
    private var mOnTagRemoveListener: OnTagRemoveListener? = null

    // the listener when a certain tag-view is clicked
    private var mOnTagClickListener: OnTagClickListener? = null

    // the possible background colors to be randomly chosen when creating a new tag-view
    private var mPossibleBackgroundColorsHashSet: HashSet<Int> = hashSetOf()
    var possibleBackgroundColors
        get() = HashSet(mPossibleBackgroundColorsHashSet)
        set(value) {
            mPossibleBackgroundColorsHashSet.clear()
            mPossibleBackgroundColorsHashSet.addAll(value)
        }

    /* =================================================================== */

    // primary constructor
    init {
        // inflate the layout
        LayoutInflater.from(context).inflate(R.layout.view_tag_cloud, this)

        // get views
        mFblTagsContainer = findViewById(R.id.fbl_tags_container)
        mImgbtnAddTag = findViewById(R.id.imgbtn_add_new_tag)

        // set events
        mImgbtnAddTag.setOnClickListener {
            val edtNewTag = EditText(context)
            edtNewTag.hint = context.getString(R.string.str_type_new_tag_here)

            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.str_add_new_tag)
                .setView(edtNewTag)
                .setPositiveButton(R.string.str_submit, null)
                .setNegativeButton(R.string.str_cancel, null)
                .create()

            dialog.setOnShowListener {
                val posBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    if (addTag(edtNewTag.text.toString()))
                        dialog.dismiss()
                }
            }

            dialog.show()
        }

        // get attributes
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.TagCloud)
            isIndicator = ta.getBoolean(R.styleable.TagCloud_is_indicator, false)
            ta.recycle()
        }
    }

    /* =================================================================== */

    /** adders & removers */

    // add a new tag
    fun addTag(tagString: String): Boolean {
        // create the tag-view
        val tagView = if (mPossibleBackgroundColorsHashSet.isEmpty()) TagView(context, tagString, mIsIndicator) else TagView(context, tagString, mIsIndicator, mPossibleBackgroundColorsHashSet)
        // set the on-remove-listener of this tag-view
        tagView.setOnRemoveListener(object: TagView.OnRemoveListener {
            override fun onRemove() {
                for ((idx, it) in mFblTagsContainer.children.filter { it is TagView }.withIndex()) {
                    if (it == tagView) {
                        removeTagByIndex(idx)
                        break
                    }
                }
            }
        })

        // set the on-click-listener of this tag-view
        tagView.setOnClickListener { mOnTagClickListener?.onTagClick(
            this@TagCloud,
            tagView,
            mFblTagsContainer.children.indexOf(tagView)
        ) }

        // the string has already been added
        return if (containsTag(tagString)) {
            mOnTagStringConflictListener?.onTagStringConflict(this, tagString)
            false
        }
        // no problem
        else {
            // add the created tag-view into the container
            mFblTagsContainer.addView(tagView, mFblTagsContainer.childCount - 1)
            // add the string of new tag into the hash-set
            mTagStringsHashMap[tagString] = tagView
            true
        }
    }

    // remove a certain tag-view by index
    fun removeTagByIndex(index: Int): Boolean {
        if (index < 0 || index >= mFblTagsContainer.childCount - 1)
            return false

        val tagView = (mFblTagsContainer.getChildAt(index) as? TagView) ?: return false
        mFblTagsContainer.removeView(tagView)
        mOnTagRemoveListener?.onTagRemove(this@TagCloud, tagView, index, mFblTagsContainer.childCount - 1)
        mTagStringsHashMap.remove(tagView.tagString)

        return true
    }

    // remove a certain tag-view by string
    fun removeTagByString(tagString: String): Boolean {
        for ((idx, it) in mFblTagsContainer.children.filter { it is TagView }.withIndex())
            if (it is TagView && it.tagString == tagString)
                return removeTagByIndex(idx)
        return false
    }

    /* =================================================================== */

    /** getters */

    // get the tag-view by index
    fun getTagAt(index: Int): TagView? = if (index >= 0 && index < mFblTagsContainer.childCount - 1)
        mFblTagsContainer.getChildAt(index) as? TagView?
    else
        null

    // get the tag string of a tag-view by index
    fun getTagStringAt(index: Int): String? = getTagAt(index)?.tagString

    // get strings of all tags
    fun getAllTagStrings(): List<String> = mFblTagsContainer
        .children
        .filter { it is TagView }
        .map { (it as TagView).tagString }
        .toList()

    // check if this tag-cloud has a certain tag by its string
    fun containsTag(tagString: String): Boolean = mTagStringsHashMap.containsKey(tagString)

    /* =================================================================== */

    /** setters */

    // set the listener when a new tag's string has already added before
    fun setOnTagStringConflictListener(onTagStringConflictListener: OnTagStringConflictListener) {
        mOnTagStringConflictListener = onTagStringConflictListener
    }

    // set the listener when a certain tag-view is about to be removed
    fun setOnTagRemoveListener(onTagRemoveListener: OnTagRemoveListener) {
        mOnTagRemoveListener = onTagRemoveListener
    }

    // the listener when a certain tag-view is clicked
    fun setOnTagClickListener(onTagClickListener: OnTagClickListener) {
        mOnTagClickListener = onTagClickListener
    }
}