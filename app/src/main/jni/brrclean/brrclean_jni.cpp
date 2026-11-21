// SPDX-License-Identifier: MIT
// Native GTCRN/STFT implementation from Poise Android (2014cd0d), under MIT.
// This glue is brrBoard-specific. It runs exclusively in :brr_clean_denoise.
#include "gtcrn.h"
#include "stft.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <jni.h>
#include <memory>
#include <vector>

namespace {
constexpr int kRate = 16000;
constexpr int kHop = 256;
constexpr int kMaxPcmBytes = kRate * 2 * 12;
constexpr int kSpectrumSize = 257;

int16_t toPcm(float sample) {
    const float clamped = std::max(-1.0f, std::min(1.0f, sample));
    return static_cast<int16_t>(std::lrint(clamped * 32767.0f));
}
} // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_helium314_keyboard_latin_voice_BrrCleanNative_process(
    JNIEnv *env, jclass, jbyteArray pcm, jbyteArray weights) {
    if (!pcm || !weights) return nullptr;
    const jsize size = env->GetArrayLength(pcm);
    const jsize weightSize = env->GetArrayLength(weights);
    if (size < kRate || size > kMaxPcmBytes || (size & 1) || weightSize < 100000 ||
        weightSize > 300000) return nullptr;

    // GetByteArrayRegion avoids holding a moving GC array during native inference.
    std::vector<jbyte> bytes(size);
    std::vector<jbyte> modelBytes(weightSize);
    env->GetByteArrayRegion(pcm, 0, size, bytes.data());
    env->GetByteArrayRegion(weights, 0, weightSize, modelBytes.data());
    if (env->ExceptionCheck()) return nullptr;

    std::unique_ptr<Gtcrn, decltype(&gtcrn_destroy)> model(
        gtcrn_create(modelBytes.data(), static_cast<size_t>(weightSize)), gtcrn_destroy);
    std::fill(modelBytes.begin(), modelBytes.end(), 0);
    if (!model) return nullptr;

    poise::STFTProcessor stft;
    const int samples = size / 2;
    const int frames = (samples + kHop - 1) / kHop;
    std::vector<jbyte> enhanced(size);
    float frame[kHop] = {};
    float real[kSpectrumSize], imag[kSpectrumSize];
    float mix[kSpectrumSize * 2], clean[kSpectrumSize * 2];
    float out[kHop];

    // The causal STFT emits a 256-sample delayed signal. The last zero-padded
    // frame flushes the tail, so original/enhanced have identical sample alignment.
    for (int step = 0; step <= frames; ++step) {
        std::fill(frame, frame + kHop, 0.0f);
        if (step < frames) {
            for (int index = 0; index < kHop; ++index) {
                const int n = step * kHop + index;
                if (n >= samples) break;
                const uint8_t lo = static_cast<uint8_t>(bytes[n * 2]);
                const uint8_t hi = static_cast<uint8_t>(bytes[n * 2 + 1]);
                frame[index] = static_cast<int16_t>(lo | (hi << 8)) / 32768.0f;
            }
        }
        stft.computeSTFT(frame, real, imag);
        for (int bin = 0; bin < kSpectrumSize; ++bin) {
            mix[bin * 2] = real[bin];
            mix[bin * 2 + 1] = imag[bin];
        }
        gtcrn_run(model.get(), mix, clean);
        for (int bin = 0; bin < kSpectrumSize; ++bin) {
            if (!std::isfinite(clean[bin * 2]) || !std::isfinite(clean[bin * 2 + 1]))
                return nullptr;
            real[bin] = clean[bin * 2];
            imag[bin] = clean[bin * 2 + 1];
        }
        stft.reconstructAudio(real, imag, out);
        if (step == 0) continue; // discard initial STFT look-behind latency
        for (int index = 0; index < kHop; ++index) {
            const int n = (step - 1) * kHop + index;
            if (n >= samples) break;
            if (!std::isfinite(out[index])) return nullptr;
            const auto value = static_cast<uint16_t>(toPcm(out[index]));
            enhanced[n * 2] = static_cast<jbyte>(value & 0xff);
            enhanced[n * 2 + 1] = static_cast<jbyte>(value >> 8);
        }
    }
    std::fill(bytes.begin(), bytes.end(), 0);
    jbyteArray result = env->NewByteArray(size);
    if (result) env->SetByteArrayRegion(result, 0, size, enhanced.data());
    std::fill(enhanced.begin(), enhanced.end(), 0);
    return result;
}
