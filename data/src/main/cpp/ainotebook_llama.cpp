// JNI glue between LlamaInferenceEngine (Kotlin) and llama.cpp.
//
// Targets the CURRENT llama.cpp API (2025). If you vendor an older/newer commit, a few symbols may
// differ — see "API compatibility notes" in README.md for the exact old↔new names to adjust.
//
// The three exported functions match the `external fun` declarations in LlamaInferenceEngine.kt.

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "llama.h"

#define LOG_TAG "ainotebook_llama"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct LlamaSession {
    llama_model   *model = nullptr;
    llama_context *ctx   = nullptr;
    int            n_ctx = 0;
};

static bool g_backend_initialized = false;

// Returns the byte length of the longest complete UTF-8 prefix of `s`, so we never hand a partial
// multi-byte sequence to NewStringUTF (which would abort).
static size_t utf8_complete_len(const std::string &s) {
    size_t i = 0;
    while (i < s.size()) {
        unsigned char c = static_cast<unsigned char>(s[i]);
        size_t clen;
        if (c < 0x80) clen = 1;
        else if ((c >> 5) == 0x6) clen = 2;
        else if ((c >> 4) == 0xE) clen = 3;
        else if ((c >> 3) == 0x1E) clen = 4;
        else clen = 1; // invalid lead byte; consume one to make progress
        if (i + clen > s.size()) break; // incomplete trailing sequence — keep for next token
        i += clen;
    }
    return i;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeLoadModel(
        JNIEnv *env, jobject /* thiz */, jstring modelPath, jint contextLength, jint threads,
        jint gpuLayers) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = gpuLayers;

    llama_model *model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (model == nullptr) {
        LOGE("Failed to load model");
        return 0;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = static_cast<uint32_t>(contextLength);
    cparams.n_threads       = threads;
    cparams.n_threads_batch = threads;

    llama_context *ctx = llama_init_from_model(model, cparams);
    if (ctx == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(model);
        return 0;
    }

    auto *session = new LlamaSession();
    session->model = model;
    session->ctx   = ctx;
    session->n_ctx = contextLength;
    LOGI("Model loaded (n_ctx=%d, threads=%d)", contextLength, threads);
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT void JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeGenerate(
        JNIEnv *env, jobject /* thiz */, jlong handle, jstring prompt, jint maxTokens,
        jfloat temperature, jfloat topP, jint topK, jfloat repeatPenalty, jobject callback) {
    auto *session = reinterpret_cast<LlamaSession *>(handle);
    if (session == nullptr || session->ctx == nullptr) return;

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)Z");
    if (onToken == nullptr) return;

    const llama_vocab *vocab = llama_model_get_vocab(session->model);

    // Tokenize the prompt (parse_special = true so ChatML <|im_start|>/<|im_end|> are handled).
    const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
    const int   promptLen   = env->GetStringUTFLength(prompt);
    int n_needed = -llama_tokenize(vocab, promptChars, promptLen, nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_needed);
    llama_tokenize(vocab, promptChars, promptLen, tokens.data(),
                   static_cast<int32_t>(tokens.size()), true, true);
    env->ReleaseStringUTFChars(prompt, promptChars);

    if (tokens.empty() || static_cast<int>(tokens.size()) >= session->n_ctx) {
        LOGE("Prompt too long for context (%zu tokens)", tokens.size());
        return;
    }

    // Build a sampler chain from the request parameters.
    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(64, repeatPenalty, 0.0f, 0.0f));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));
    llama_token id = 0; // stable storage; the per-token batch points at &id
    std::string pending;
    int n_decoded = 0;
    bool stop = false;

    while (!stop && n_decoded < maxTokens) {
        if (llama_decode(session->ctx, batch) != 0) {
            LOGE("llama_decode failed");
            break;
        }
        id = llama_sampler_sample(smpl, session->ctx, -1);
        if (llama_vocab_is_eog(vocab, id)) break;

        char piece[512];
        int n = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, true);
        if (n < 0) break;
        pending.append(piece, static_cast<size_t>(n));

        size_t good = utf8_complete_len(pending);
        if (good > 0) {
            std::string chunk = pending.substr(0, good);
            pending.erase(0, good);
            jstring jchunk = env->NewStringUTF(chunk.c_str());
            jboolean cont = env->CallBooleanMethod(callback, onToken, jchunk);
            env->DeleteLocalRef(jchunk);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                break;
            }
            if (cont == JNI_FALSE) stop = true;
        }

        n_decoded++;
        batch = llama_batch_get_one(&id, 1);
    }

    llama_sampler_free(smpl);
}

extern "C" JNIEXPORT void JNICALL
Java_com_debayan_ainotebook_data_ai_LlamaInferenceEngine_nativeFree(
        JNIEnv * /* env */, jobject /* thiz */, jlong handle) {
    auto *session = reinterpret_cast<LlamaSession *>(handle);
    if (session == nullptr) return;
    if (session->ctx != nullptr) llama_free(session->ctx);
    if (session->model != nullptr) llama_model_free(session->model);
    delete session;
}
