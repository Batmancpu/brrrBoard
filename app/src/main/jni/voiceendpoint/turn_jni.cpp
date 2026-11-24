// SPDX-License-Identifier: GPL-3.0-only
// Reuse the existing sherpa-onnx package's public ONNX Runtime C ABI.
#include <jni.h>
#include <dlfcn.h>
#include <new>
#include <limits>
#include <cstdint>
#include <onnxruntime_c_api.h>

namespace {
constexpr int64_t kShape[] = {1, 80, 800};
constexpr size_t kFeatures = 80 * 800;
constexpr size_t kModelBytes = 8679182;
struct Turn {
    void* lib = nullptr;
    const OrtApi* api = nullptr;
    OrtEnv* env = nullptr;
    OrtSessionOptions* options = nullptr;
    OrtSession* session = nullptr;
    OrtMemoryInfo* memory = nullptr;
    OrtAllocator* allocator = nullptr;
    char* outputName = nullptr;
    ~Turn() {
        if (api) {
            if (outputName && allocator) ok(api->AllocatorFree(allocator, outputName));
            if (memory) api->ReleaseMemoryInfo(memory);
            if (session) api->ReleaseSession(session);
            if (options) api->ReleaseSessionOptions(options);
            if (env) api->ReleaseEnv(env);
        }
        if (lib) dlclose(lib);
    }
    bool ok(OrtStatus* status) {
        if (!status) return true;
        api->ReleaseStatus(status);
        return false;
    }
    bool initialize(const void* bytes, size_t len) {
        lib = dlopen("libonnxruntime.so", RTLD_NOW | RTLD_LOCAL);
        if (!lib) return false;
        auto getBase = reinterpret_cast<decltype(&OrtGetApiBase)>(dlsym(lib, "OrtGetApiBase"));
        if (!getBase || !getBase()) return false;
        api = getBase()->GetApi(ORT_API_VERSION);
        if (!api) return false;
        if (!ok(api->CreateEnv(ORT_LOGGING_LEVEL_ERROR, "brr-turn", &env)) ||
            !ok(api->CreateSessionOptions(&options)) ||
            !ok(api->SetIntraOpNumThreads(options, 1)) ||
            !ok(api->SetInterOpNumThreads(options, 1)) ||
            !ok(api->SetSessionGraphOptimizationLevel(options, ORT_ENABLE_ALL)) ||
            !ok(api->CreateSessionFromArray(env, bytes, len, options, &session)) ||
            !ok(api->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &memory)) ||
            !ok(api->GetAllocatorWithDefaultOptions(&allocator))) return false;
        size_t inputs = 0, outputs = 0;
        if (!ok(api->SessionGetInputCount(session, &inputs)) ||
            !ok(api->SessionGetOutputCount(session, &outputs)) ||
            inputs != 1 || outputs != 1) return false;
        return ok(api->SessionGetOutputName(session, 0, allocator, &outputName)) && outputName;
    }
    float score(float* features) {
        const float invalid = std::numeric_limits<float>::quiet_NaN();
        OrtValue* input = nullptr;
        OrtValue* output = nullptr;
        OrtTensorTypeAndShapeInfo* shape = nullptr;
        float value = invalid;
        const char* inputName = "input_features";
        const OrtValue* inputs[1] = {};
        const char* outputs[1] = { outputName };
        if (ok(api->CreateTensorWithDataAsOrtValue(memory, features, kFeatures * sizeof(float),
                kShape, 3, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &input))) {
            inputs[0] = input;
            if (ok(api->Run(session, nullptr, &inputName, inputs, 1, outputs, 1, &output)) &&
                ok(api->GetTensorTypeAndShape(output, &shape))) {
                size_t count = 0;
                ONNXTensorElementDataType type = ONNX_TENSOR_ELEMENT_DATA_TYPE_UNDEFINED;
                void* result = nullptr;
                if (ok(api->GetTensorShapeElementCount(shape, &count)) && count == 1 &&
                    ok(api->GetTensorElementType(shape, &type)) &&
                    type == ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT &&
                    ok(api->GetTensorMutableData(output, &result)) && result)
                    value = *static_cast<float*>(result);
            }
        }
        if (shape) api->ReleaseTensorTypeAndShapeInfo(shape);
        if (output) api->ReleaseValue(output);
        if (input) api->ReleaseValue(input);
        return value;
    }
};
}
extern "C" JNIEXPORT jlong JNICALL
Java_helium314_keyboard_latin_voice_BrrVoiceTurnNative_create(JNIEnv* env, jobject, jbyteArray weights) {
    if (!weights || static_cast<size_t>(env->GetArrayLength(weights)) != kModelBytes) return 0;
    auto* state = new (std::nothrow) Turn();
    if (!state) return 0;
    jbyte* bytes = env->GetByteArrayElements(weights, nullptr);
    if (!bytes) { delete state; return 0; }
    bool valid = state->initialize(bytes, kModelBytes);
    env->ReleaseByteArrayElements(weights, bytes, JNI_ABORT);
    if (!valid) { delete state; return 0; }
    return reinterpret_cast<jlong>(state);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_helium314_keyboard_latin_voice_BrrVoiceTurnNative_score(JNIEnv* env, jobject, jlong handle, jfloatArray features) {
    if (!handle || !features || static_cast<size_t>(env->GetArrayLength(features)) != kFeatures)
        return std::numeric_limits<float>::quiet_NaN();
    auto* state = reinterpret_cast<Turn*>(handle);
    jfloat* values = env->GetFloatArrayElements(features, nullptr);
    if (!values) return std::numeric_limits<float>::quiet_NaN();
    float result = state->score(values);
    env->ReleaseFloatArrayElements(features, values, JNI_ABORT);
    return result;
}
extern "C" JNIEXPORT void JNICALL
Java_helium314_keyboard_latin_voice_BrrVoiceTurnNative_release(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Turn*>(handle);
}
