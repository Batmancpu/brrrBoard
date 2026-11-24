// SPDX-License-Identifier: MIT
// Streaming adapter for the pinned Poise GTCRN/STFT implementation.
#ifndef BRR_CLEAN_STREAM_H
#define BRR_CLEAN_STREAM_H
#include "gtcrn.h"
#include "stft.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <vector>

class BrrCleanStream {
public:
    static constexpr int kHop = 256;
    static constexpr int kHopBytes = kHop * 2;
    static constexpr int kBins = 257;
    BrrCleanStream(const void *weights, size_t len)
        : model_(gtcrn_create(weights, len), gtcrn_destroy) {}
    ~BrrCleanStream() { if (model_) gtcrn_reset(model_.get()); stft_.reset(); }
    bool valid() const { return model_ && !failed_; }
    // Each packet is a whole number of hops. The first output discards one
    // causal STFT hop; the caller provides the lookahead and flushes at EOS.
    // Never feed a duplicated overlap or reset the recurrent state mid-stream.
    bool push(const uint8_t *pcm, size_t bytes, std::vector<uint8_t>& result) {
        if (!valid() || !pcm || bytes == 0 || bytes % kHopBytes) return false;
        result.clear();
        result.reserve(bytes);
        float frame[kHop], real[kBins], imag[kBins], mix[kBins*2], clean[kBins*2], out[kHop];
        for (size_t offset=0; offset<bytes; offset+=kHopBytes) {
            for (int i=0; i<kHop; ++i) {
                uint16_t v = static_cast<uint16_t>(pcm[offset+i*2]) |
                             (static_cast<uint16_t>(pcm[offset+i*2+1]) << 8);
                frame[i] = static_cast<int16_t>(v) / 32768.0f;
            }
            stft_.computeSTFT(frame, real, imag);
            for (int i=0; i<kBins; ++i) { mix[2*i]=real[i]; mix[2*i+1]=imag[i]; }
            gtcrn_run(model_.get(),mix,clean);
            for (int i=0; i<kBins; ++i) {
                if (!std::isfinite(clean[2*i]) || !std::isfinite(clean[2*i+1])) {
                    failed_=true; return false;
                }
                real[i]=clean[2*i]; imag[i]=clean[2*i+1];
            }
            stft_.reconstructAudio(real,imag,out);
            const bool omit=first_;
            first_=false;
            for (float v : out) {
                if (!std::isfinite(v)) { failed_=true; return false; }
                if (!omit) {
                    const auto n=static_cast<uint16_t>(static_cast<int16_t>(
                        std::lrint(std::max(-1.0f,std::min(1.0f,v))*32767.0f)));
                    result.push_back(static_cast<uint8_t>(n&255));
                    result.push_back(static_cast<uint8_t>(n>>8));
                }
            }
        }
        return true;
    }
private:
    std::unique_ptr<Gtcrn,decltype(&gtcrn_destroy)> model_;
    poise::STFTProcessor stft_;
    bool first_=true;
    bool failed_=false;
};
#endif
