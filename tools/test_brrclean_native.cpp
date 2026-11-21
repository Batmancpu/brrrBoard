// SPDX-License-Identifier: GPL-3.0-only
// Host smoke: pinned weights + real C++ GTCRN/STFT, no Android device or mic.
#include "gtcrn.h"
#include "stft.h"
#include <cmath>
#include <cstdio>
#include <fstream>
#include <iterator>
#include <memory>
#include <vector>

int main(int argc, char **argv) {
    if (argc != 2) return 2;
    std::ifstream file(argv[1], std::ios::binary);
    if (!file) return 3;
    const std::vector<char> bytes((std::istreambuf_iterator<char>(file)),
                                  std::istreambuf_iterator<char>());
    std::unique_ptr<Gtcrn, decltype(&gtcrn_destroy)> model(
        gtcrn_create(bytes.data(), bytes.size()), gtcrn_destroy);
    if (!model) return 4;
    poise::STFTProcessor transform;
    constexpr int hop = 256;
    float frame[hop], real[257], imag[257], mix[514], result[514], output[hop];
    double inputEnergy = 0, resultEnergy = 0;
    int seen = 0;
    for (int pass = 0; pass < 80; ++pass) {
        for (int i = 0; i < hop; ++i) {
            const float t = (pass * hop + i) / 16000.0f;
            frame[i] = .2f * std::sin(2 * 3.14159265f * 230 * t) +
                       .02f * std::sin(2 * 3.14159265f * 1900 * t);
            inputEnergy += frame[i] * frame[i];
        }
        transform.computeSTFT(frame, real, imag);
        for (int i = 0; i < 257; ++i) {
            mix[i * 2] = real[i];
            mix[i * 2 + 1] = imag[i];
        }
        gtcrn_run(model.get(), mix, result);
        for (int i = 0; i < 257; ++i) {
            if (!std::isfinite(result[i * 2]) || !std::isfinite(result[i * 2 + 1])) return 5;
            real[i] = result[i * 2];
            imag[i] = result[i * 2 + 1];
        }
        transform.reconstructAudio(real, imag, output);
        for (float sample : output) {
            if (!std::isfinite(sample) || std::abs(sample) > 4) return 6;
            resultEnergy += sample * sample;
            ++seen;
        }
    }
    if (seen != 80 * hop || resultEnergy < inputEnergy * 0.0025) return 7;
    std::printf("brrClean host smoke passed: %d finite samples, energy ratio %.3f\n",
                seen, resultEnergy / inputEnergy);
    return 0;
}
