/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.keyboard.emoji;

import android.util.SparseIntArray;
import android.view.View;
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
    private int smartRowInset;
    // A detached RecyclerView from the old editor must never restore a row
    // after the new session has cleared its navigation memory.
    private long scrollMemoryGeneration;

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

    void setSmartRowInset(final int insetPx) {
        final int bounded = Math.max(0, insetPx);
        if (smartRowInset == bounded) return;
        smartRowInset = bounded;
        final int recentsPosition = indexOf(EmojiCategory.ID_RECENTS);
        if (recentsPosition >= 0 && recentsPosition < getItemCount())
            notifyItemChanged(recentsPosition);
    }

    static int rowTopPadding(final int headerPx, final int breathingPx,
            final int smartInsetPx, final boolean recents) {
        return Math.max(0, headerPx) + Math.max(0, breathingPx)
                + (recents ? Math.max(0, smartInsetPx) : 0);
    }

    void refresh() { notifyDataSetChanged(); }

    void resetScrollMemory() {
        ++scrollMemoryGeneration;
        rememberedRows.clear();
        notifyDataSetChanged();
    }

    static boolean acceptsScrollMemory(final long boundGeneration,
            final long currentGeneration) {
        return boundGeneration == currentGeneration;
    }

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
        // Reuse nearby rows while flinging instead of recreating keyboard
        // drawing views at every small direction reversal. Bounded per page.
        rows.setItemViewCacheSize(4);
        rows.setClipToPadding(false);
        rows.setVerticalScrollBarEnabled(false);
        frame.addView(rows);
        return new PageHolder(frame, rows);
    }

    @Override public void onBindViewHolder(@NonNull final PageHolder holder, final int position) {
        final int categoryId = categoryAt(position);
        holder.categoryId = categoryId;
        holder.scrollMemoryGeneration = scrollMemoryGeneration;
        final long boundGeneration = scrollMemoryGeneration;
        // A page can be rebound while the IME switches between compact Browse
        // and the taller Search keyboard. Do not carry its old pixel offset
        // into the first row of the newly measured viewport: only row indices
        // are persisted, so position zero must begin wholly below top padding.
        if (holder.topAnchorListener != null) {
            holder.rows.removeOnLayoutChangeListener(holder.topAnchorListener);
        }
        holder.topAnchorListener = null;
        final float density = holder.rows.getResources().getDisplayMetrics().density;
        // The category tabs now OVERLAY the same live scrolling surface.
        // Start row zero EXACTLY below the fixed category header on first
        // open. An extra 8dp "breathing" inset compounded the perceived
        // blank band on compact phones. clipToPadding=false still lets rows
        // slide behind the TOP glass when scrolled; no Browse spacer exists.
        final int topPadding = rowTopPadding(holder.rows.getResources().getDimensionPixelSize(
                helium314.keyboard.latin.R.dimen.config_suggestions_strip_height),
                0, smartRowInset,
                categoryId == EmojiCategory.ID_RECENTS);
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
                if (!acceptsScrollMemory(boundGeneration, scrollMemoryGeneration)) return;
                rowScrollCallback.onRowScrollStateChanged(categoryId, state);
            }
            @Override public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (holder.categoryId != categoryId
                        || !acceptsScrollMemory(boundGeneration, scrollMemoryGeneration)) return;
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
        final View.OnLayoutChangeListener topAnchorListener = new View.OnLayoutChangeListener() {
            private boolean corrected;

            @Override public void onLayoutChange(final View view, final int left, final int top,
                    final int right, final int bottom, final int oldLeft, final int oldTop,
                    final int oldRight, final int oldBottom) {
                // A one-time geometry correction, not a scroll snap. Ordinary
                // finger scrolling and saved nonzero category rows are untouched.
                if (corrected || holder.categoryId != categoryId ||
                        !acceptsScrollMemory(boundGeneration, scrollMemoryGeneration) ||
                        bottom <= top || holder.rows.getScrollState() != RecyclerView.SCROLL_STATE_IDLE ||
                        rememberedRows.get(categoryId, 0) != 0) return;
                final LinearLayoutManager lm = (LinearLayoutManager) holder.rows.getLayoutManager();
                if (lm == null || lm.findFirstVisibleItemPosition() != 0) return;
                final View first = lm.findViewByPosition(0);
                if (first == null) return;
                if (EmojiRowViewportPolicy.firstRowNeedsAlignment(first.getTop(),
                        holder.rows.getPaddingTop())) {
                    corrected = true;
                    lm.scrollToPositionWithOffset(0, 0);
                    holder.rows.requestLayout();
                }
            }
        };
        holder.topAnchorListener = topAnchorListener;
        holder.rows.addOnLayoutChangeListener(topAnchorListener);
        // Rebinding a page does not always change RecyclerView's outer bounds,
        // so run the same check once after its next child layout as well.
        holder.rows.post(() -> {
            if (holder.topAnchorListener == topAnchorListener
                    && acceptsScrollMemory(boundGeneration, scrollMemoryGeneration)) {
                topAnchorListener.onLayoutChange(holder.rows, 0, 0,
                        holder.rows.getWidth(), holder.rows.getHeight(), 0, 0, 0, 0);
            }
        });
    }

    @Override public void onViewRecycled(@NonNull final PageHolder holder) {
        if (holder.topAnchorListener != null) {
            holder.rows.removeOnLayoutChangeListener(holder.topAnchorListener);
            holder.topAnchorListener = null;
        }
        remember(holder);
        holder.rows.setAdapter(null);
    }

    @Override public void onViewDetachedFromWindow(@NonNull final PageHolder holder) { remember(holder); }

    private void remember(final PageHolder holder) {
        if (holder.categoryId < 0 ||
                !acceptsScrollMemory(holder.scrollMemoryGeneration, scrollMemoryGeneration)) return;
        final LinearLayoutManager manager = (LinearLayoutManager) holder.rows.getLayoutManager();
        if (manager == null) return;
        final int row = manager.findFirstVisibleItemPosition();
        if (row != RecyclerView.NO_POSITION) rememberedRows.put(holder.categoryId, row);
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        final RecyclerView rows;
        View.OnLayoutChangeListener topAnchorListener;
        int categoryId = -1;
        long scrollMemoryGeneration;
        PageHolder(final FrameLayout root, final RecyclerView rows) { super(root); this.rows = rows; }
    }
}
