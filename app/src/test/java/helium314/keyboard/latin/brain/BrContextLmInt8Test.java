// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.common.ComposedData;
import helium314.keyboard.latin.dictionary.Dictionary;
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** A zero-weight model proves the native learned-history feature reaches neural scoring. */
@RunWith(RobolectricTestRunner.class)
public class BrContextLmInt8Test {
    private static final class FakeDictionary extends Dictionary {
        FakeDictionary(final String type) { super(type, null); }

        @Override public ArrayList<SuggestedWordInfo> getSuggestions(
                final ComposedData data, final NgramContext context, final long layout,
                final SettingsValuesForSuggestion settings, final int session, final float weight,
                final float[] override) {
            return null;
        }
        @Override public boolean isInDictionary(final String word) { return false; }
    }

    private static final class ModelBytes {
        final byte[] manifest;
        final byte[] vocabulary;
        final byte[] weights;

        ModelBytes(final byte[] manifest, final byte[] vocabulary, final byte[] weights) {
            this.manifest = manifest;
            this.vocabulary = vocabulary;
            this.weights = weights;
        }

        BrContextLmInt8 load() throws IOException {
            return BrContextLmInt8.load(new ByteArrayInputStream(manifest),
                    new ByteArrayInputStream(vocabulary), new ByteArrayInputStream(weights));
        }
    }

    private static String sha256(final byte[] data) throws Exception {
        final byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        final StringBuilder result = new StringBuilder(64);
        for (byte b : hash) result.append(String.format("%02x", b & 0xff));
        return result.toString();
    }

    private static void add(final JSONArray tensors, final ByteArrayOutputStream binary,
                            final String name, final int nonzeroAt, final int... shape) throws Exception {
        int elements = 1;
        final JSONArray dimensions = new JSONArray();
        for (int size : shape) {
            dimensions.put(size);
            elements *= size;
        }
        final int offset = binary.size();
        for (int i = 0; i < elements; i++) binary.write(i == nonzeroAt ? 3 : 0);
        tensors.put(new JSONObject().put("name", name).put("shape", dimensions)
                .put("scale", 1).put("bytes", elements).put("offset", offset));
    }

    private static ModelBytes fixture() throws Exception {
        final JSONObject words = new JSONObject();
        words.put("<pad>", 0).put("<unk>", 1).put("<bos>", 2).put("<eos>", 3)
                .put("kal", 4).put("meeting", 5).put("office", 6);
        final byte[] vocab = words.toString().getBytes(StandardCharsets.UTF_8);
        final ByteArrayOutputStream weights = new ByteArrayOutputStream();
        final JSONArray params = new JSONArray();
        add(params, weights, "embedding.weight", -1, 7, 128);
        add(params, weights, "memories", -1, 5, 4, 128);
        for (int i = 0; i < 5; i++) {
            add(params, weights, "projections." + i + ".weight", -1, 128, 128);
            add(params, weights, "projections." + i + ".bias", -1, 128);
        }
        // A model can trust inherited native history directly, even with no lexical weights.
        add(params, weights, "personal_base", 2, 4);
        add(params, weights, "personal_gate.weight", -1, 4, 128);
        add(params, weights, "personal_gate.bias", -1, 4);
        final byte[] raw = weights.toByteArray();
        final byte[] manifest = new JSONObject()
                .put("status", "APPROVED_FOR_BUNDLING")
                .put("do_not_ship_until_accuracy_latency_licensing_size_tests_pass", false)
                .put("architecture", "causal_dfsmn_5x128_taps4_candidate_head")
                .put("vocab_count", 7)
                .put("vocab_sha256", sha256(vocab))
                .put("weights_sha256", sha256(raw))
                .put("parameters", params)
                .toString().getBytes(StandardCharsets.UTF_8);
        return new ModelBytes(manifest, vocab, raw);
    }

    private static SuggestedWordInfo word(final String value, final String type) {
        return new SuggestedWordInfo(value, "", 185, SuggestedWordInfo.KIND_PREDICTION,
                new FakeDictionary(type), SuggestedWordInfo.NOT_AN_INDEX,
                SuggestedWordInfo.NOT_A_CONFIDENCE);
    }

    @Test public void inheritedHistoryEntersModelScoreBeforeAnySafetyFusion() throws Exception {
        final BrContextLmInt8 reference = fixture().load();
        final NgramContext previous = new NgramContext(new NgramContext.WordInfo("kal"));
        final BrPersonalContextFeatures.Snapshot snapshot = BrPersonalContextFeatures.fromNative(
                previous, Arrays.asList(word("meeting", Dictionary.TYPE_USER_HISTORY),
                        word("office", Dictionary.TYPE_MAIN)), true, true);
        assertTrue(snapshot.hasInheritedPersonalSignals);
        final float[] scores = reference.score(snapshot);
        assertEquals(2, scores.length);
        assertEquals(3f, scores[0], 0.001f);
        assertEquals(0f, scores[1], 0.001f);
        final BrPersonalContextFeatures.Snapshot off = BrPersonalContextFeatures.fromNative(
                previous, Arrays.asList(word("meeting", Dictionary.TYPE_USER_HISTORY),
                        word("office", Dictionary.TYPE_MAIN)), true, false);
        assertFalse(off.hasInheritedPersonalSignals);
        assertEquals(1, reference.score(off).length);
    }

    @Test(expected = IOException.class)
    public void modifiedModelWeightsAreRejectedBeforeScoring() throws Exception {
        final ModelBytes model = fixture();
        model.weights[0] = 1;
        model.load();
    }

    @Test(expected = IOException.class)
    public void experimentalTrainingExportIsNeverExecutable() throws Exception {
        final ModelBytes model = fixture();
        final JSONObject manifest = new JSONObject(new String(model.manifest, StandardCharsets.UTF_8))
                .put("status", "UNVALIDATED_TRAINING_EXPERIMENT_NOT_FOR_APK")
                .put("do_not_ship_until_accuracy_latency_licensing_size_tests_pass", true);
        BrContextLmInt8.load(new ByteArrayInputStream(manifest.toString().getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(model.vocabulary),
                new ByteArrayInputStream(model.weights));
    }
}
