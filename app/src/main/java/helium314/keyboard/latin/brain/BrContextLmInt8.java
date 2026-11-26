// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Small CPU-only reference executor for BR's causal DFSMN candidate scorer.
 *
 * <p>All disk/asset reads, digest verification and model construction MUST happen on a bounded
 * background worker; never construct this object on the IME rendering/key thread. The INT8
 * weights are expanded once to floats, avoiding an extra native runtime or JNI crash surface.
 * The production loader requires release approval. A separate explicit test loader accepts only
 * a manifest marked experimental, quality-gate-failed, and manual-suggestions-only.
 */
public final class BrContextLmInt8 {
    public static final int CONTEXT_TOKENS = 12;
    public static final int CHANNELS = 128;
    public static final int LAYERS = 5;
    public static final int MEMORY_TAPS = 4;
    private static final int MAX_MODEL_BYTES = 8 * 1024 * 1024;
    private static final int MAX_VOCAB_BYTES = 512 * 1024;
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final int MAX_VOCAB_TOKENS = 16384;
    private static final String EXPECTED_ARCH = "causal_dfsmn_5x128_taps4_candidate_head";

    private final Map<String, Integer> vocabulary;
    private final float[] embedding;
    private final float[][] projections;
    private final float[][] biases;
    private final float[][] memory;
    private final float[] personalBase;
    private final float[] personalGateWeights;
    private final float[] personalGateBias;
    private final int vocabSize;

    private BrContextLmInt8(final Map<String, Integer> vocabulary,
                            final Map<String, Weights> parameters) throws IOException {
        this.vocabulary = vocabulary;
        vocabSize = vocabulary.size();
        embedding = parameter(parameters, "embedding.weight", vocabSize, CHANNELS);
        projections = new float[LAYERS][];
        biases = new float[LAYERS][];
        for (int i = 0; i < LAYERS; i++) {
            projections[i] = parameter(parameters, "projections." + i + ".weight", CHANNELS, CHANNELS);
            biases[i] = parameter(parameters, "projections." + i + ".bias", CHANNELS);
        }
        memory = new float[LAYERS][MEMORY_TAPS * CHANNELS];
        final float[] fullMemory = parameter(parameters, "memories", LAYERS, MEMORY_TAPS, CHANNELS);
        for (int i = 0; i < LAYERS; i++) {
            System.arraycopy(fullMemory, i * MEMORY_TAPS * CHANNELS,
                    memory[i], 0, MEMORY_TAPS * CHANNELS);
        }
        personalBase = parameter(parameters, "personal_base", 4);
        personalGateWeights = parameter(parameters, "personal_gate.weight", 4, CHANNELS);
        personalGateBias = parameter(parameters, "personal_gate.bias", 4);
    }

    private static final class Weights {
        final float[] values;
        final int[] shape;

        Weights(final float[] values, final int[] shape) {
            this.values = values;
            this.shape = shape;
        }
    }

    private static float[] parameter(final Map<String, Weights> parameters,
                                     final String name, final int... expectedShape) throws IOException {
        final Weights result = parameters.get(name);
        if (result == null || result.shape.length != expectedShape.length) {
            throw new IOException("Missing/malformed ContextLM parameter: " + name);
        }
        for (int i = 0; i < expectedShape.length; i++) {
            if (result.shape[i] != expectedShape[i]) {
                throw new IOException("Invalid ContextLM parameter shape: " + name);
            }
        }
        return result.values;
    }

    /** Caller owns all streams; only an authorized background worker should invoke this. */
    @NonNull public static BrContextLmInt8 load(@NonNull final InputStream manifestStream,
                                                @NonNull final InputStream vocabStream,
                                                @NonNull final InputStream modelStream) throws IOException {
        return loadInternal(manifestStream, vocabStream, modelStream, false);
    }

    /**
     * Loads an explicitly experimental model for the user-visible BR Brain test mode only.
     * This never grants permission to use the model for automatic correction or normal ranking.
     */
    @NonNull public static BrContextLmInt8 loadExperimentalForTesting(
            @NonNull final InputStream manifestStream,
            @NonNull final InputStream vocabStream,
            @NonNull final InputStream modelStream) throws IOException {
        return loadInternal(manifestStream, vocabStream, modelStream, true);
    }

    private static BrContextLmInt8 loadInternal(@NonNull final InputStream manifestStream,
                                                @NonNull final InputStream vocabStream,
                                                @NonNull final InputStream modelStream,
                                                final boolean allowExperimental) throws IOException {
        try {
            final byte[] manifestBytes = readBounded(manifestStream, MAX_MANIFEST_BYTES);
            final byte[] vocabBytes = readBounded(vocabStream, MAX_VOCAB_BYTES);
            final byte[] modelBytes = readBounded(modelStream, MAX_MODEL_BYTES);
            final JSONObject manifest = new JSONObject(new String(manifestBytes, StandardCharsets.UTF_8));
            // Training exports are deliberately marked experimental. Accidentally
            // copying one into assets must never silently activate an unvalidated model.
            final boolean approved = "APPROVED_FOR_BUNDLING".equals(manifest.optString("status"))
                    && !manifest.optBoolean("do_not_ship_until_accuracy_latency_licensing_size_tests_pass", true);
            final boolean experimental = allowExperimental
                    && "EXPERIMENTAL_DEVICE_TEST_ONLY".equals(manifest.optString("status"))
                    && !manifest.optBoolean("quality_gate_passed", true)
                    && manifest.optBoolean("do_not_use_for_autocorrection", false)
                    && manifest.optBoolean("apply_only_to_manual_suggestions", false);
            if (allowExperimental ? !experimental : !approved) {
                throw new IOException("ContextLM has not passed release approval");
            }
            if (!EXPECTED_ARCH.equals(manifest.optString("architecture"))) {
                throw new IOException("Unknown ContextLM architecture");
            }
            if (!digestMatches(vocabBytes, manifest.getString("vocab_sha256"))
                    || !digestMatches(modelBytes, manifest.getString("weights_sha256"))) {
                throw new IOException("ContextLM asset hash mismatch");
            }
            final JSONObject words = new JSONObject(new String(vocabBytes, StandardCharsets.UTF_8));
            if (words.length() > MAX_VOCAB_TOKENS) {
                throw new IOException("Oversized ContextLM vocabulary");
            }
            final Map<String, Integer> vocabulary = new HashMap<>();
            final boolean[] tokenIdsSeen = new boolean[words.length()];
            for (java.util.Iterator<String> it = words.keys(); it.hasNext();) {
                final String word = it.next();
                final int index = words.getInt(word);
                if (index < 0 || index >= words.length() || tokenIdsSeen[index]) {
                    throw new IOException("Invalid ContextLM token index");
                }
                tokenIdsSeen[index] = true;
                vocabulary.put(word, index);
            }
            if (vocabulary.size() != manifest.getInt("vocab_count")
                    || !Integer.valueOf(1).equals(vocabulary.get("<unk>"))
                    || !Integer.valueOf(2).equals(vocabulary.get("<bos>"))) {
                throw new IOException("Invalid ContextLM vocabulary manifest");
            }
            final Map<String, Weights> parameters = new HashMap<>();
            final JSONArray entries = manifest.getJSONArray("parameters");
            if (entries.length() > 64) throw new IOException("Unbounded ContextLM parameter count");
            for (int i = 0; i < entries.length(); i++) {
                final JSONObject entry = entries.getJSONObject(i);
                final String name = entry.getString("name");
                final JSONArray dimensions = entry.getJSONArray("shape");
                if (dimensions.length() < 1 || dimensions.length() > 3) {
                    throw new IOException("Invalid ContextLM tensor dimension");
                }
                final int[] shape = new int[dimensions.length()];
                long total = 1;
                for (int d = 0; d < shape.length; d++) {
                    shape[d] = dimensions.getInt(d);
                    if (shape[d] <= 0 || shape[d] > 65536) throw new IOException("Invalid ContextLM shape");
                    total *= shape[d];
                }
                final int offset = entry.getInt("offset");
                final int bytes = entry.getInt("bytes");
                final double scale = entry.getDouble("scale");
                if (total != bytes || offset < 0 || bytes < 0
                        || (long) offset + bytes > modelBytes.length
                        || !Double.isFinite(scale) || scale <= 0 || scale > 1000
                        || parameters.containsKey(name)) {
                    throw new IOException("Invalid ContextLM tensor boundary");
                }
                final float[] values = new float[bytes];
                for (int j = 0; j < bytes; j++) {
                    values[j] = modelBytes[offset + j] * (float) scale;
                }
                parameters.put(name, new Weights(values, shape));
            }
            return new BrContextLmInt8(vocabulary, parameters);
        } catch (JSONException | NoSuchAlgorithmException | IllegalArgumentException e) {
            throw new IOException("Invalid ContextLM model metadata", e);
        }
    }

    private static byte[] readBounded(final InputStream stream, final int maxBytes)
            throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] scratch = new byte[8192];
        int count;
        while ((count = stream.read(scratch)) != -1) {
            if (count == 0) throw new IOException("ContextLM asset stream made no progress");
            if (output.size() > maxBytes - count) throw new IOException("ContextLM asset too large");
            output.write(scratch, 0, count);
        }
        return output.toByteArray();
    }

    private static boolean digestMatches(final byte[] contents, final String expected)
            throws NoSuchAlgorithmException {
        if (!expected.matches("[a-f0-9]{64}")) return false;
        final byte[] digest = MessageDigest.getInstance("SHA-256").digest(contents);
        final char[] hex = "0123456789abcdef".toCharArray();
        final StringBuilder actual = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            actual.append(hex[(value >> 4) & 15]).append(hex[value & 15]);
        }
        return expected.contentEquals(actual);
    }

    /** One exact-context memo, owned by the experimental editor scope, never persisted. */
    static final class ContextCache {
        private volatile Encoding last;
        private int computations;
        int computationsForTesting() { return computations; }
    }

    private static final class Encoding {
        final int[] tokenIds;
        final float[] latest;
        final float[] personalWeights;
        Encoding(int[] tokenIds, float[] latest, float[] personalWeights) {
            this.tokenIds = tokenIds;
            this.latest = latest;
            this.personalWeights = personalWeights;
        }
    }

    boolean knowsWord(String word) {
        final Integer id = vocabulary.get(word.toLowerCase(Locale.ROOT));
        return id != null && id >= 4;
    }

    /** Scores existing native candidate words without generating a new auto-correction. */
    @NonNull public float[] score(@NonNull final BrPersonalContextFeatures.Snapshot snapshot) {
        return score(snapshot, null);
    }

    @NonNull float[] score(@NonNull final BrPersonalContextFeatures.Snapshot snapshot,
                           final ContextCache cache) {
        return score(snapshot, cache, null, -1L);
    }

    @Nullable float[] score(@NonNull final BrPersonalContextFeatures.Snapshot snapshot,
                           final ContextCache cache, @Nullable final LongSupplier requestSerialSupplier,
                           final long requestSerial) {
        if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
        if (snapshot.candidates.size() > BrPersonalContextFeatures.MAX_CANDIDATES) {
            throw new IllegalArgumentException("Too many candidate words");
        }
        if (snapshot.candidates.isEmpty()) return new float[0];
        // A ContextLM result must never be published without a separate editor-generation check.
        final List<String> recent = snapshot.previousWords;
        final int[] tokenIds = new int[CONTEXT_TOKENS];
        final int begin = vocabulary.get("<bos>");
        for (int i = 0; i < CONTEXT_TOKENS; i++) tokenIds[i] = begin;
        for (int i = 0; i < recent.size() && i < CONTEXT_TOKENS; i++) {
            if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
            final String word = recent.get(recent.size() - 1 - i).toLowerCase(Locale.ROOT);
            tokenIds[CONTEXT_TOKENS - 1 - i] = vocabulary.getOrDefault(word, 1);
        }
        Encoding encoding = cache == null ? null : cache.last;
        if (encoding == null || !Arrays.equals(encoding.tokenIds, tokenIds)) {
            encoding = encodeContext(tokenIds, requestSerialSupplier, requestSerial);
            if (encoding == null || requestIsStale(requestSerialSupplier, requestSerial)) return null;
            if (cache != null) {
                cache.last = encoding;
                cache.computations++;
            }
        }
        final float[] latest = encoding.latest;
        final float[] personalWeights = encoding.personalWeights;
        final float[] scores = new float[snapshot.candidates.size()];
        for (int i = 0; i < scores.length; i++) {
            if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
            final BrPersonalContextFeatures.Candidate candidate = snapshot.candidates.get(i);
            final int wordId = vocabulary.getOrDefault(candidate.word.toLowerCase(Locale.ROOT), 1);
            float sum = 0;
            for (int channel = 0; channel < CHANNELS; channel++) {
                sum += latest[channel] * embedding[wordId * CHANNELS + channel];
            }
            sum /= (float) Math.sqrt(CHANNELS);
            sum += personalWeights[0] * candidate.nativeScore;
            sum += personalWeights[1] * candidate.nativeRank;
            sum += personalWeights[2] * candidate.learnedHistory;
            sum += personalWeights[3] * candidate.personalWord;
            scores[i] = Float.isFinite(sum) ? sum : Float.NEGATIVE_INFINITY;
        }
        return scores;
    }

    private Encoding encodeContext(final int[] tokenIds, @Nullable final LongSupplier requestSerialSupplier,
            final long requestSerial) {
        final int stateSize = CONTEXT_TOKENS * CHANNELS;
        float[] states = new float[stateSize];
        float[] following = new float[stateSize];
        for (int i = 0; i < CONTEXT_TOKENS; i++) {
            if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
            final int index = tokenIds[i] * CHANNELS;
            System.arraycopy(embedding, index, states, i * CHANNELS, CHANNELS);
        }
        for (int layer = 0; layer < LAYERS; layer++) {
            if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
            final float[] matrix = projections[layer];
            final float[] bias = biases[layer];
            for (int t = 0; t < CONTEXT_TOKENS; t++) {
                if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
                final int stateOffset = t * CHANNELS;
                for (int output = 0; output < CHANNELS; output++) {
                    float value = bias[output];
                    final int row = output * CHANNELS;
                    for (int input = 0; input < CHANNELS; input++) {
                        value += matrix[row + input] * states[stateOffset + input];
                    }
                    for (int lag = 1; lag <= MEMORY_TAPS && lag <= t; lag++) {
                        value += states[stateOffset - lag * CHANNELS + output]
                                * memory[layer][(lag - 1) * CHANNELS + output];
                    }
                    following[stateOffset + output] = (float) Math.tanh(value);
                }
            }
            final float[] previous = states;
            states = following;
            following = previous;
        }
        if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
        final float[] latest = new float[CHANNELS];
        System.arraycopy(states, (CONTEXT_TOKENS - 1) * CHANNELS, latest, 0, CHANNELS);
        final float[] personalWeights = new float[4];
        for (int feature = 0; feature < 4; feature++) {
            float gate = personalGateBias[feature];
            for (int channel = 0; channel < CHANNELS; channel++) {
                gate += personalGateWeights[feature * CHANNELS + channel] * latest[channel];
            }
            personalWeights[feature] = personalBase[feature] + (float) Math.tanh(gate);
        }
        if (requestIsStale(requestSerialSupplier, requestSerial)) return null;
        return new Encoding(tokenIds, latest, personalWeights);
    }

    private static boolean requestIsStale(@Nullable final LongSupplier requestSerialSupplier,
            final long requestSerial) {
        return requestSerialSupplier != null && requestSerialSupplier.getAsLong() != requestSerial;
    }
}
