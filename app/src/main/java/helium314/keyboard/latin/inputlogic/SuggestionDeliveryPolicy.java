// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import java.util.Objects;

/** A pure, content-free gate for delivering background results to the visible IME. */
final class SuggestionDeliveryPolicy {
    private SuggestionDeliveryPolicy() {}

    static boolean isCurrent(final long expectedEditor, final long currentEditor,
            final long expectedRequest, final long currentRequest,
            final String expectedTypedWord, final String currentTypedWord,
            final int expectedSelectionStart, final int currentSelectionStart,
            final int expectedSelectionEnd, final int currentSelectionEnd) {
        return expectedEditor == currentEditor && expectedRequest == currentRequest
                && Objects.equals(expectedTypedWord, currentTypedWord)
                && expectedSelectionStart == currentSelectionStart
                && expectedSelectionEnd == currentSelectionEnd;
    }
}
