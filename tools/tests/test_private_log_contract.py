"""Regression guard against dumping keyboard data to Android/in-app logs."""

from pathlib import Path
import unittest


JAVA = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "java"
LATIN = JAVA / "helium314" / "keyboard" / "latin"


class PrivateLogContractTest(unittest.TestCase):
    def test_known_user_content_logging_cannot_return(self):
        forbidden_fragments = {
            "RichInputConnection.java": ['"did set " + text'],
            "inputlogic/InputLogic.java": [
                '"commitChosenWord() : [" + chosenWord',
                '"NgramContext = " + ngramContext',
            ],
            "dictionary/ExpandableBinaryDictionary.java": [
                '"Cannot add unigram entry. word: " + word',
                '"Cannot remove unigram entry: " + word',
                '"  NgramContext: " + ngramContext',
                '"Cannot update counter. word: " + word',
                'Log.d(tag, wordProperty.toString())',
            ],
            "dictionary/AppsBinaryDictionary.java": [
                '"addName word = " + word',
                '"addName " + appLabel',
            ],
            "dictionary/ContactsBinaryDictionary.java": [
                '"addName word = " + word',
                '"addName " + name',
            ],
            "spellcheck/AndroidWordLevelSpellCheckerSession.java": [
                '"onGetSuggestionsInternal() : [" + text',
            ],
            "database/ClipboardDao.kt": [
                '"can\'t delete cached clipboard image: $file"',
                '"can\'t resolve cached clipboard image: $uri"',
            ],
            "DictionaryFacilitatorImpl.kt": ['to add word \\"$word\\"', 'to remove word \\"$word\\"'],
            "Suggest.kt": ['"replaced batch word $pseudoTypedWordInfo'],
            "stickers/StickerContentProvider.kt": ['"query: $uri'],
            "utils/LayoutUtilsCustom.kt": ['"too long text on key: ${it.mLabel}', '"too long text on popup key: ${popupKey.mLabel}'],
        }
        for relative_path, fragments in forbidden_fragments.items():
            content = (LATIN / relative_path).read_text()
            for fragment in fragments:
                with self.subTest(file=relative_path, fragment=fragment):
                    self.assertNotIn(fragment, content)

    def test_private_dictionary_dump_reports_no_entries(self):
        source = (LATIN / "dictionary" / "ExpandableBinaryDictionary.java").read_text()
        method = source.split("public void dumpAllWordsForDebug()", 1)[1].split("\n    }", 1)[0]
        self.assertNotIn("getNextWordProperty", method)
        self.assertNotIn("wordProperty", method)
        self.assertIn("disabled", method)


if __name__ == "__main__":
    unittest.main()
