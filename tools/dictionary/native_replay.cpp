// SPDX-License-Identifier: GPL-3.0-only
#include <algorithm>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>

#include "dictionary/property/ngram_context.h"
#include "dictionary/property/word_property.h"
#include "dictionary/structure/dictionary_structure_with_buffer_policy_factory.h"
#include "dictionary/structure/v2/patricia_trie_policy.h"
#include "suggest/core/dictionary/dictionary.h"
#include "suggest/core/layout/proximity_info.h"
#include "suggest/core/result/suggestion_results.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/core/suggest_options.h"

using namespace latinime;

// Same array boundary as Android's JNI decoder, backed by host memory.
struct IntArray : _jintArray {
    explicit IntArray(size_t count, int fill = 0) : values(count, fill) {}
    std::vector<jint> values;
};

static JNINativeInterface_ hostJni() {
    JNINativeInterface_ functions{};
    functions.FindClass = [](JNIEnv *, const char *) -> jclass { return nullptr; };
    functions.ExceptionClear = [](JNIEnv *) {};
    functions.GetArrayLength = [](JNIEnv *, jarray array) {
        return static_cast<jsize>(static_cast<IntArray *>(array)->values.size());
    };
    functions.GetIntArrayRegion = [](JNIEnv *, jintArray array, jsize start, jsize n, jint *output) {
        std::copy_n(static_cast<IntArray *>(array)->values.begin() + start, n, output);
    };
    functions.SetIntArrayRegion = [](JNIEnv *, jintArray array, jsize start, jsize n, const jint *input) {
        std::copy_n(input, n, static_cast<IntArray *>(array)->values.begin() + start);
    };
    functions.SetFloatArrayRegion = [](JNIEnv *, jfloatArray, jsize, jsize, const jfloat *) {};
    return functions;
}

static std::string ascii(const CodePointArrayView word) {
    std::string result;
    for (const int cp : word) {
        if (cp == 0x2019) result += '\'';
        else if (cp >= 32 && cp <= 126) result += static_cast<char>(cp);
        else return {};
    }
    return result;
}

struct Result {
    std::string word;
    int score;
    unsigned flags;
};

static std::vector<Result> collect(JNIEnv *env, SuggestionResults &suggestions, int capacity) {
    IntArray count(1), codepoints(capacity * MAX_WORD_LENGTH), scores(capacity);
    IntArray space(capacity), types(capacity), confidence(1);
    _jfloatArray weight;
    suggestions.outputSuggestions(env, &count, &codepoints, &scores, &space, &types, &confidence, &weight);
    std::vector<Result> result;
    for (int i = 0; i < count.values[0]; ++i) {
        const auto start = codepoints.values.data() + i * MAX_WORD_LENGTH;
        const auto end = std::find(start, start + MAX_WORD_LENGTH, 0);
        auto word = ascii(CodePointArrayView(start, end - start));
        if (!word.empty()) result.push_back({std::move(word), scores.values[i], unsigned(types.values[i])});
    }
    std::sort(result.begin(), result.end(), [](const Result &a, const Result &b) {
        return std::make_tuple(-static_cast<long long>(a.score), a.word.size(), a.word) <
                std::make_tuple(-static_cast<long long>(b.score), b.word.size(), b.word);
    });
    return result;
}

static int inventory(JNIEnv *env, Dictionary &dictionary) {
    int token = 0;
    do {
        int points[MAX_WORD_LENGTH];
        int count = 0;
        token = dictionary.getNextWordAndNextToken(token, points, &count);
        if (count <= 0) return 2;
        const CodePointArrayView codepoints(points, count);
        const auto word = ascii(codepoints);
        if (word.empty()) continue;
        const auto property = dictionary.getWordProperty(codepoints);
        const auto &unigram = property.getUnigramProperty();
        if (unigram.isNotAWord() || unigram.isPossiblyOffensive()) continue;
        const NgramContext context(points, count, false);
        constexpr int capacity = 18;
        SuggestionResults predictions(capacity);
        dictionary.getPredictions(&context, &predictions);
        const auto next = collect(env, predictions, capacity);
        std::cout << word << '\t' << unigram.getProbability();
        for (size_t i = 0; i < std::min<size_t>(next.size(), 3); ++i) {
            std::cout << '\t' << next[i].word;
        }
        std::cout << '\n';
    } while (token != 0);
    return dictionary.getDictionaryStructurePolicy()->isCorrupted() || !std::cout ? 2 : 0;
}

static std::vector<int> points(const std::string &word) {
    return std::vector<int>(word.begin(), word.end());
}

static int tapReplay(JNIEnv *env, const std::string &fixtures, Dictionary &stock, Dictionary &supplement) {
    constexpr int columns = 10, rows = 3, keySize = 100, keys = 27;
    IntArray x(keys), y(keys), w(keys, keySize), h(keys, keySize), codes(keys);
    IntArray grid(columns * rows * MAX_PROXIMITY_CHARS_SIZE, NOT_A_CODE_POINT);
    const std::vector<std::string> layout = {"qwertyuiop", "asdfghjkl", "zxcvbnm'"};
    int k = 0;
    for (size_t row = 0; row < layout.size(); ++row) {
        for (size_t col = 0; col < layout[row].size(); ++col, ++k) {
            x.values[k] = col * keySize;
            y.values[k] = row * keySize;
            codes.values[k] = layout[row][col];
            const int cell = (row * columns + col) * MAX_PROXIMITY_CHARS_SIZE;
            grid.values[cell] = codes.values[k];
        }
    }
    ProximityInfo proximity(env, columns * keySize, rows * keySize, columns, rows,
            keySize, keySize, &grid, keys, &x, &y, &w, &h, &codes, nullptr, nullptr, nullptr);
    std::ifstream input(fixtures);
    if (!input) return 2;
    for (std::string line; std::getline(input, line);) {
        if (line.empty() || line[0] == '#') continue;
        std::istringstream fields(line);
        std::string previous, literal, expected;
        if (!std::getline(fields, previous, '\t') || !std::getline(fields, literal, '\t') ||
                !std::getline(fields, expected, '\t')) return 2;
        auto contextPoints = points(previous);
        const NgramContext context(contextPoints.data(), contextPoints.size(), false);
        auto codepoints = points(literal);
        std::vector<int> xs, ys, times, pointers(codepoints.size(), 0);
        for (int cp : codepoints) {
            int key = proximity.getKeyIndexOf(cp);
            xs.push_back(key == NOT_AN_INDEX ? -1 : x.values[key] + keySize / 2);
            ys.push_back(key == NOT_AN_INDEX ? -1 : y.values[key] + keySize / 2);
            times.push_back(times.size() * 25);
        }
        const int options[] = {0, 1, 1, 0, 1000};
        const SuggestOptions opts(options, NELEMS(options));
        constexpr int capacity = 18;
        std::cout << previous << '\t' << literal << '\t' << expected;
        for (auto *dictionary : {&stock, &supplement}) {
            SuggestionResults suggestions(capacity);
            DicTraverseSession session(env, nullptr, true);
            dictionary->getSuggestions(&proximity, &session, xs.data(), ys.data(), times.data(),
                    pointers.data(), codepoints.data(), codepoints.size(), &context, &opts,
                    NOT_A_WEIGHT_OF_LANG_MODEL_VS_SPATIAL_MODEL, &suggestions);
            const auto result = collect(env, suggestions, capacity);
            std::cout << '\t';
            bool first = true;
            for (const auto &entry : result) {
                if (!first) std::cout << '|';
                std::cout << entry.word << ':' << entry.score << ':' << entry.flags;
                first = false;
            }
        }
        std::cout << '\n';
    }
    return input.bad() || !std::cout ? 2 : 0;
}

int main(int argc, char **argv) {
    if (argc != 2 && argc != 4) return 2;
    auto buffer = MmappedBuffer::openBuffer(argv[1], false);
    if (!buffer || FormatUtils::detectFormatVersion(buffer->getReadOnlyByteArrayView())
            != FormatUtils::VERSION_202) return 2;
    const auto functions = hostJni();
    JNIEnv env{&functions};
    Dictionary stock(&env, std::make_unique<PatriciaTriePolicy>(std::move(buffer)));
    if (argc == 2) return inventory(&env, stock);

    std::ifstream lexical(argv[3]);
    if (!lexical) return 2;
    DictionaryHeaderStructurePolicy::AttributeMap attributes;
    auto supplementPolicy = DictionaryStructureWithBufferPolicyFactory::newPolicyForOnMemoryDict(
            FormatUtils::VERSION_403, points("en"), &attributes);
    if (!supplementPolicy) return 2;
    Dictionary supplement(&env, std::move(supplementPolicy));
    for (std::string line; std::getline(lexical, line);) {
        if (line.empty() || line[0] == '#') continue;
        std::istringstream fields(line);
        std::string locale, word, frequency, policy;
        if (!std::getline(fields, locale, '\t') || !std::getline(fields, word, '\t') ||
                !std::getline(fields, frequency, '\t') || !std::getline(fields, policy)) return 2;
        const UnigramProperty props(false, false, false, false, std::stoi(frequency), HistoricalInfo());
        const auto chars = points(word);
        if (!supplement.addUnigramEntry(CodePointArrayView(chars), &props) ||
                supplement.getProbability(CodePointArrayView(chars)) < 0) return 2;
    }
    return tapReplay(&env, argv[2], stock, supplement);
}
