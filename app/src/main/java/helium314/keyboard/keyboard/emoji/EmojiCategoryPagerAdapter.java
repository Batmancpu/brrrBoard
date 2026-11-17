/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.keyboard.emoji;

import android.util.SparseIntArray;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Each category owns a whole horizontal page and its own vertical virtualized
 * row list. ViewPager2 owns finger tracking/snap; row lists own vertical fling.
 * Only the current and adjacent pages need to be inflated.
 */
final class EmojiCategoryPagerAdapter extends RecyclerView.Adapter<EmojiCategoryPagerAdapter.PageHolder> {
    interface RowScrollCallback {
        void onCategoryRowScrolled(int categoryId, int firstRow);
        void onRowScrollStateChanged(int categoryId, int state);
    }

    private final EmojiCategory category;
    private final EmojiViewCallback emojiCallback;
    private final SparseIntArray rememberedRows;
    private final RowScrollCallback rowScrollCallback;
    private final List<EmojiCategory.CategoryProperties> pages;
    private int horizontalPadding;

    EmojiCategoryPagerAdapter(final EmojiCategory category, final EmojiViewCallback emojiCallback,
            final SparseIntArray rememberedRows, final RowScrollCallback rowScrollCallback) {
        this.category = category;
        this.emojiCallback = emojiCallback;
        this.rememberedRows = rememberedRows;
        this.rowScrollCallback = rowScrollCallback;
        pages = category.getShownCategories();
        setHasStableIds(true);
    }

    int indexOf(final int categoryId) {
        return category.getTabIdFromCategoryId(categoryId);
    }

    int categoryAt(final int position) {
        return pages.get(position).mCategoryId;
    }

    void setHorizontalPadding(final int padding) {
        horizontalPadding = Math.max(0, padding);
        notifyDataSetChanged();
    }

    void refresh() { notifyDataSetChanged(); }

    @Override public long getItemId(final int position) { return categoryAt(position); }

    @Override public int getItemCount() { return pages.size(); }

    @NonNull
    @Override public PageHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final FrameLayout frame = new FrameLayout(parent.getContext());
        frame.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final RecyclerView rows = new RecyclerView(parent.getContext());
        rows.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rows.setLayoutManager(new LinearLayoutManager(parent.getContext(), RecyclerView.VERTICAL, false));
        rows.setItemAnimator(null);
        rows.setHasFixedSize(true);
        rows.setClipToPadding(false);
        rows.setVerticalScrollBarEnabled(false);
        frame.addView(rows);
        return new PageHolder(frame, rows);
    }

    @Override public void onBindViewHolder(@NonNull final PageHolder holder, final int position) {
        final int categoryId = categoryAt(position);
        holder.categoryId = categoryId;
        final float density = holder.rows.getResources().getDisplayMetrics().density;
        // Categories now sit in the fixed strip ABOVE this viewport. Keep only
        // the original 8dp breathing room; the old 60dp reservation for an
        // overlaid Search Emoji pill caused a huge blank band before row one.
        final int topPadding = (int) (8 * density);
        final int bottomPadding = (int) (50 * density)
                + holder.rows.getResources().getDimensionPixelSize(
                        helium314.keyboard.latin.R.dimen.config_board_footer_height)
                + (int) (8 * density);
        holder.rows.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
        final EmojiPalettesAdapter rowAdapter = new EmojiPalettesAdapter(category, emojiCallback);
        rowAdapter.setCategory(categoryId);
        holder.rows.setAdapter(rowAdapter);
        holder.rows.clearOnScrollListeners();
        holder.rows.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int state) {
                rowScrollCallback.onRowScrollStateChanged(categoryId, state);
            }
            @Override public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (holder.categoryId != categoryId) return;
                final LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager == null) return;
                final int first = manager.findFirstVisibleItemPosition();
                if (first == RecyclerView.NO_POSITION) return;
                rememberedRows.put(categoryId, first);
                rowScrollCallback.onCategoryRowScrolled(categoryId, first);
            }
        });
        final LinearLayoutManager manager = (LinearLayoutManager) holder.rows.getLayoutManager();
        if (manager != null) manager.scrollToPositionWithOffset(
                Math.max(0, Math.min(rowAdapter.getItemCount() - 1, rememberedRows.get(categoryId, 0))), 0);
    }

    @Override public void onViewRecycled(@NonNull final PageHolder holder) {
        remember(holder);
        holder.rows.setAdapter(null);
    }

    @Override public void onViewDetachedFromWindow(@NonNull final PageHolder holder) { remember(holder); }

    private void remember(final PageHolder holder) {
        if (holder.categoryId < 0) return;
        final LinearLayoutManager manager = (LinearLayoutManager) holder.rows.getLayoutManager();
        if (manager == null) return;
        final int row = manager.findFirstVisibleItemPosition();
        if (row != RecyclerView.NO_POSITION) rememberedRows.put(holder.categoryId, row);
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        final RecyclerView rows;
        int categoryId = -1;
        PageHolder(final FrameLayout root, final RecyclerView rows) { super(root); this.rows = rows; }
    }
}
