/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.latin.R;

/** One full-width horizontal page per emoji grid. */
final class EmojiPalettesAdapter extends RecyclerView.Adapter<EmojiPalettesAdapter.PageViewHolder> {
    private final EmojiViewCallback mEmojiViewCallback;
    private final EmojiCategory mEmojiCategory;
    private final List<PageItem> mItems = new ArrayList<>();

    private static final class PageItem {
        final int categoryId;
        final int pagePosition;

        PageItem(int categoryId, int pagePosition) {
            this.categoryId = categoryId;
            this.pagePosition = pagePosition;
        }

        long getStableId() {
            return (((long) categoryId) << 32) | pagePosition;
        }
    }

    EmojiPalettesAdapter(final EmojiCategory emojiCategory, final EmojiViewCallback emojiViewCallback) {
        mEmojiCategory = emojiCategory;
        mEmojiViewCallback = emojiViewCallback;
        setHasStableIds(true);
        updateItems();
    }

    void updateItems() {
        mItems.clear();
        for (final EmojiCategory.CategoryProperties properties : mEmojiCategory.getShownCategories()) {
            final int categoryId = properties.mCategoryId;
            for (int page = 0; page < properties.getPageCount(); page++) {
                mItems.add(new PageItem(categoryId, page));
            }
        }
        notifyDataSetChanged();
    }

    int getCategoryIdForPosition(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).categoryId : -1;
    }

    int getPagePositionForAdapterPosition(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).pagePosition : -1;
    }

    int getAdapterPositionForCategoryPage(int categoryId, int pagePosition) {
        for (int i = 0; i < mItems.size(); i++) {
            final PageItem item = mItems.get(i);
            if (item.categoryId == categoryId && item.pagePosition == pagePosition) {
                return i;
            }
        }
        return getFirstPagePositionOfCategory(categoryId);
    }

    int getFirstPagePositionOfCategory(int categoryId) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).categoryId == categoryId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getStableId();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final EmojiPageKeyboardView keyboardView = (EmojiPageKeyboardView) inflater.inflate(
                R.layout.emoji_keyboard_page, parent, false);
        keyboardView.setEmojiViewCallback(mEmojiViewCallback);
        keyboardView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new PageViewHolder(keyboardView);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        final PageItem item = mItems.get(position);
        final Keyboard keyboard = mEmojiCategory.getKeyboardFromAdapterPosition(item.categoryId, item.pagePosition);
        holder.keyboardView.setKeyboard(keyboard);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PageViewHolder holder) {
        holder.keyboardView.releaseCurrentKey(false);
        holder.keyboardView.deallocateMemory();
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        holder.keyboardView.releaseCurrentKey(false);
        holder.keyboardView.deallocateMemory();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static final class PageViewHolder extends RecyclerView.ViewHolder {
        final EmojiPageKeyboardView keyboardView;

        PageViewHolder(View view) {
            super(view);
            keyboardView = (EmojiPageKeyboardView) view;
        }
    }
}
