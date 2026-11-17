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
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.latin.R;

/**
 * B10V: one virtualized row per RecyclerView item, only for the selected
 * category. No horizontal pages/snapping; native vertical fling works inside
 * each category, while horizontal gestures are owned by the parent panel.
 */
final class EmojiPalettesAdapter extends RecyclerView.Adapter<EmojiPalettesAdapter.RowViewHolder> {
    private final EmojiViewCallback mCallback;
    private final EmojiCategory mCategory;
    private int mCategoryId;
    private int mRowCount;

    EmojiPalettesAdapter(final EmojiCategory category, final EmojiViewCallback callback) {
        mCategory = category;
        mCallback = callback;
        mCategoryId = category.getCurrentCategoryId();
        setHasStableIds(true);
        updateItems();
    }

    void setCategory(final int categoryId) {
        if (mCategoryId == categoryId) return;
        mCategoryId = categoryId;
        updateItems();
    }

    void updateItems() {
        final int count = mCategoryId == EmojiCategory.ID_RECENTS
                ? mCategory.getKeyboard(mCategoryId, 0).getSortedKeys().size()
                : mCategory.getCategoryKeyCount(mCategoryId);
        mRowCount = EmojiRowPolicy.rowCount(count, mCategory.getPageColumnCount());
        notifyDataSetChanged();
    }

    int getCategoryIdForPosition(final int position) {
        return position >= 0 && position < mRowCount ? mCategoryId : -1;
    }

    int getPagePositionForAdapterPosition(final int position) {
        return position >= 0 && position < mRowCount
                ? position / Math.max(1, mCategory.getRowsPerPage()) : -1;
    }

    int getAdapterPositionForCategoryPage(final int categoryId, final int page) {
        return categoryId == mCategoryId
                ? Math.min(mRowCount - 1, Math.max(0, page) * mCategory.getRowsPerPage()) : -1;
    }

    @Override
    public long getItemId(final int position) {
        return (((long) mCategoryId) << 32) | position;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final EmojiPageKeyboardView view = (EmojiPageKeyboardView) LayoutInflater
                .from(parent.getContext()).inflate(R.layout.emoji_keyboard_page, parent, false);
        view.setEmojiViewCallback(mCallback);
        view.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RowViewHolder holder, final int position) {
        final Keyboard keyboard = mCategory.getRowKeyboard(mCategoryId, position);
        holder.keyboardView.setKeyboard(keyboard);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull final RowViewHolder holder) {
        holder.keyboardView.releaseCurrentKey(false);
        holder.keyboardView.deallocateMemory();
    }

    @Override
    public void onViewRecycled(@NonNull final RowViewHolder holder) {
        holder.keyboardView.releaseCurrentKey(false);
        holder.keyboardView.deallocateMemory();
    }

    @Override
    public int getItemCount() { return mRowCount; }

    static final class RowViewHolder extends RecyclerView.ViewHolder {
        final EmojiPageKeyboardView keyboardView;

        RowViewHolder(final View view) {
            super(view);
            keyboardView = (EmojiPageKeyboardView) view;
        }
    }
}
