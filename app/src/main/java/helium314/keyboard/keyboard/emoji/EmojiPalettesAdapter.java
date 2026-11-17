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

/** Virtualized, freely scrolling columns of a fixed-height Emoji grid. */
final class EmojiPalettesAdapter extends RecyclerView.Adapter<EmojiPalettesAdapter.PageViewHolder> {
    private final EmojiViewCallback mEmojiViewCallback;
    private final EmojiCategory mEmojiCategory;
    private final List<PageItem> mItems = new ArrayList<>();

    private static final class PageItem {
        final int categoryId;
        final int pagePosition;
        final int columnPosition;

        PageItem(int categoryId, int pagePosition, int columnPosition) {
            this.categoryId = categoryId;
            this.pagePosition = pagePosition;
            this.columnPosition = columnPosition;
        }

        long getStableId() {
            return (((long) categoryId) << 48) | (((long) pagePosition) << 16) | columnPosition;
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
        final int rows = mEmojiCategory.getRowsPerPage();
        final int columns = mEmojiCategory.getPageColumnCount();
        for (final EmojiCategory.CategoryProperties properties : mEmojiCategory.getShownCategories()) {
            final int categoryId = properties.mCategoryId;
            final boolean recents = categoryId == EmojiCategory.ID_RECENTS;
            final int keyCount = recents
                    ? mEmojiCategory.getKeyboard(categoryId, 0).getSortedKeys().size()
                    : mEmojiCategory.getCategoryKeyCount(categoryId);
            final int pages;
            if (recents) {
                pages = EmojiColumnPolicy.pageCount(keyCount, columns, rows);
            } else {
                pages = properties.getPageCount();
            }
            for (int page = 0; page < pages; page++) {
                final int columnCount = EmojiColumnPolicy.columnCount(
                        keyCount, columns, rows, page);
                for (int column = 0; column < columnCount; column++) {
                    mItems.add(new PageItem(categoryId, page, column));
                }
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
        // Older versions persisted whole-page offsets. Prefer the nearest
        // available page if the category or number of columns changed.
        int lastPageStart = -1;
        for (int i = 0; i < mItems.size(); i++) {
            final PageItem item = mItems.get(i);
            if (item.categoryId == categoryId) {
                if (item.columnPosition == 0) {
                    lastPageStart = i;
                }
                if (item.pagePosition > pagePosition) {
                    return i;
                }
            }
        }
        return lastPageStart;
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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new PageViewHolder(keyboardView);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        final PageItem item = mItems.get(position);
        final Keyboard keyboard = mEmojiCategory.getColumnKeyboard(
                item.categoryId, item.pagePosition, item.columnPosition);
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
