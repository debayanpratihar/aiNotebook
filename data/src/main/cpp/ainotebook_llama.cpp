// JNI glue between LlamaInferenceEngine (Kotlin) and llama.cpp.
//
// This is the integration skeleton. It compiles only once llama.cpp is vendored into this directory
// and `#include "llama.h"` resolves; the exact llama.cpp calls below must be adapted to the API of
// the vendored version (the API evolves). Until then the Kotlin side detects the missing .so and
// degrades gracefully (see LlamaInferenceEngine.NATIVE_AVAILABLE).
//
// The three exported functions match the `external fun` declarations in LlamaInferenceEngine.

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

// #include "llama.h"   // provided by the vendored llama.cpp

#define LOG_TAG "ainotebook_llama"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Opaque session holding the loaded model + context. Replace the void* members with the concrete
// llama.cpp types (llama_model*, llama_context*) once llama.h is available.
struct LlamaSession {
    void *model = nullptr;    // llama_model*
    void *context = nullptr;  // llama_context*
    int contextLength = 0;
    int threads = 1;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeLoadModel(
        JNIEnv *env, jobject /* thiz */, jstring modelPath, jint contextLength, jint threads,
        jint gpuLayers) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    auto *session = new LlamaSession();
    session->contextLength = contextLength;
    session->threads = threads;

    // TODO(llama.cpp): initialize backend, load the model, and create a context, e.g.:
    //   llama_backend_init();
    //   llama_model_params mp = llama_model_default_params();
    //   mp.n_gpu_layers = gpuLayers;
    //   session->model = llama_load_model_from_file(path, mp);
    //   llama_context_params cp = llama_context_default_params();
    //   cp.n_ctx = contextLength; cp.n_threads = threads;
    //   session->context = llama_new_context_with_model((llama_model*) session->model, cp);
    //   if (!session->model || !session->context) { /* cleanup and fail */ }

    env->ReleaseStringUTFChars(modelPath, path);

    // Return 0 to signal failure (also the current state until llama.cpp is wired in).
    if (session->model == nullptr) {
        delete session;
        return 0;
    }
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT void JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeGenerate(
        JNIEnv *env, jobject /* thiz */, jlong handle, jstring prompt, jint maxTokens,
        jfloat temperature, jfloat topP, jint topK, jfloat repeatPenalty, jobject callback) {
    auto *session = reinterpret_cast<LlamaSession *>(handle);
    if (session == nullptr) return;

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)Z");
    if (onToken == nullptr) return;

    const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
    // const std::string promptStr(promptChars);

    // TODO(llama.cpp): tokenize the prompt, then run the decode loop. For each generated token:
    //   1. Convert the token piece to a UTF-8 std::string `piece`.
    //   2. jstring jpiece = env->NewStringUTF(piece.c_str());
    //   3. jboolean cont = env->CallBooleanMethod(callback, onToken, jpiece);
    //   4. env->DeleteLocalRef(jpiece);
    //   5. if (cont == JNI_FALSE) break;  // caller requested stop (interruption)
    //   6. stop on EOS / the ChatML <|im_end|> stop token or when maxTokens is reached.
    //
    // Sampling should apply temperature / topP / topK / repeatPenalty from the arguments above.

    env->ReleaseStringUTFChars(prompt, promptChars);
}

extern "C" JNIEXPORT void JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeFree(
        JNIEnv * /* env */, jobject /* thiz */, jlong handle) {
    auto *session = reinterpret_cast<LlamaSession *>(handle);
    if (session == nullptr) return;
    // TODO(llama.cpp): llama_free((llama_context*) session->context);
    //                  llama_free_model((llama_model*) session->model);
    delete session;
}
