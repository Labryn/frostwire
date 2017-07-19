/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.search.KeywordDetector;
import com.frostwire.search.KeywordFilter;
import com.google.android.flexbox.FlexboxLayout;

/**
 * @author aldenml
 * @author gubatron
 * @author marcelinkaaa
 */
public final class KeywordTagView extends AppCompatTextView {

    private boolean dismissible;
    private KeywordFilter keywordFilter;
    private int count;
    private KeywordTagViewListener listener;
    private TextAppearanceSpan keywordSpan;
    private TextAppearanceSpan countSpan;
    private FlexboxLayout.LayoutParams layoutParams;

    public interface KeywordTagViewListener {
        void onKeywordTagViewDismissed(KeywordTagView view);

        void onKeywordTagViewTouched(KeywordTagView view);
    }

    private KeywordTagView(Context context, AttributeSet attrs, KeywordFilter keywordFilter) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.KeywordTagView, 0, 0);
        count = attributes.getInteger(R.styleable.KeywordTagView_keyword_tag_count, 0);
        dismissible = attributes.getBoolean(R.styleable.KeywordTagView_keyword_tag_dismissable, true);
        this.keywordFilter = keywordFilter;
        attributes.recycle();

        keywordSpan = new TextAppearanceSpan(getContext(), R.style.keywordTagText);
        countSpan = new TextAppearanceSpan(getContext(), R.style.keywordTagCount);

        layoutParams = new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, toPx(34));
        layoutParams.setMargins(0, 0, toPx(6), toPx(8));

        setPadding(toPx(12), toPx(4), toPx(12), toPx(4));
        setMinHeight(toPx(34));
        setGravity(Gravity.CENTER_VERTICAL);

        setBackgroundResource(R.drawable.keyword_tag_background);
        setCompoundDrawablesWithIntrinsicBounds(R.drawable.keyword_tag_filter_add, 0, R.drawable.keyword_tag_close_clear_cancel_full, 0);
        setCompoundDrawablePadding(toPx(6));

        setText(R.string.dummy_text);
        setTextColor(ContextCompat.getColor(getContext(), R.color.app_text_primary));
    }

    public KeywordTagView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
        // dummy, to refactor soon
        keywordFilter = new KeywordFilter(true, "[Text]", KeywordDetector.Feature.MANUAL_ENTRY);
        updateComponents();
    }

    public KeywordTagView(Context context, KeywordFilter keywordFilter, int count, boolean dismissible, KeywordTagViewListener listener) {
        this(context, null, keywordFilter);
        this.count = count;
        this.dismissible = dismissible;
        this.listener = listener;
        updateComponents();
    }

    @Override
    public FlexboxLayout.LayoutParams getLayoutParams() {
        return layoutParams;
    }

    private void updateComponents() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb = append(sb, keywordFilter.getKeyword(), keywordSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (count != -1) {
            sb = append(sb, "  (" + String.valueOf(count) + ")", countSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        setText(sb, TextView.BufferType.NORMAL);

        if (isInEditMode()) {
            return;
        }

        if (dismissible) {
            setBackgroundResource(R.drawable.keyword_tag_background_active);
            int drawableResId = keywordFilter.isInclusive() ? R.drawable.keyword_tag_filter_add : R.drawable.keyword_tag_filter_minus;
            setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, R.drawable.keyword_tag_close_clear_cancel_full, 0);
            setTextColor(ContextCompat.getColor(getContext(), R.color.app_text_white));
        } else {
            setBackgroundResource(R.drawable.keyword_tag_background);
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            setTextColor(ContextCompat.getColor(getContext(), R.color.app_text_primary));
        }

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onKeywordTagViewTouched();
            }
        });
        if (dismissible) {
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() != MotionEvent.ACTION_UP) {
                        return false;
                    }
                    TextView tv = (TextView) v;
                    if (event.getX() >= tv.getWidth() - tv.getTotalPaddingRight()) {
                        onDismissed();
                        return true;
                    }
                    return false;
                }
            });
        }
        invalidate();
    }

    private void onKeywordTagViewTouched() {
        if (this.listener != null) {
            this.listener.onKeywordTagViewTouched(this);
        }
    }

    public KeywordFilter getKeywordFilter() {
        return keywordFilter;
    }

    public boolean isDismissible() {
        return dismissible;
    }

    /**
     * Replaces instance of internal KeywordFilter with one that toggles the previous one's inclusive mode
     */
    public KeywordFilter toggleFilterInclusionMode() {
        KeywordFilter oldKeywordFilter = getKeywordFilter();
        KeywordFilter newKeywordFilter = new KeywordFilter(!oldKeywordFilter.isInclusive(), oldKeywordFilter.getKeyword(), oldKeywordFilter.getFeature());
        this.keywordFilter = newKeywordFilter;
        updateComponents();
        return newKeywordFilter;
    }

    public void setListener(KeywordTagViewListener listener) {
        this.listener = listener;
    }

    private void onDismissed() {
        if (listener != null) {
            listener.onKeywordTagViewDismissed(this);
        }
    }

    private int toPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // for API 16 compatibility
    private static SpannableStringBuilder append(SpannableStringBuilder sb, CharSequence text, Object what, int flags) {
        int start = sb.length();
        sb.append(text);
        sb.setSpan(what, start, sb.length(), flags);
        return sb;
    }
}
