# Native inference (llama.cpp) — integration guide

The on-device AI runs GGUF models through **llama.cpp** compiled with the Android NDK into
`libainotebook_llama.so`. The Kotlin side (`LlamaInferenceEngine`) is complete and calls this library
through JNI. Until the library is built, `LlamaInferenceEngine.NATIVE_AVAILABLE` is `false` and AI
generation fails gracefully with a clear message — **the rest of the app builds and runs normally.**

## Enabling the native build

1. **Vendor llama.cpp** here:
   ```
   git submodule add https://github.com/ggml-org/llama.cpp data/src/main/cpp/llama.cpp
   ```
2. **CMakeLists.txt** — uncomment `add_subdirectory(llama.cpp)` and the `llama` line in
   `target_link_libraries`.
3. **ainotebook_llama.cpp** — replace the `TODO(llama.cpp)` blocks with the real API calls for the
   vendored version (`llama_load_model_from_file`, `llama_new_context_with_model`, tokenize,
   decode/sample loop, `llama_token_to_piece`, free). The generation loop must call the Kotlin
   `TokenCallback.onToken(String): Boolean` per token and stop when it returns `false`.
4. **data/build.gradle.kts** — uncomment the `externalNativeBuild` and `ndk { abiFilters … }` blocks
   (`arm64-v8a` at minimum; add `x86_64` for the emulator).

## Contract (must match `LlamaInferenceEngine`)

| Kotlin `external fun` | JNI symbol |
|---|---|
| `nativeLoadModel(path, ctxLen, threads, gpuLayers): Long` | returns an opaque session handle, or `0` on failure |
| `nativeGenerate(handle, prompt, maxTokens, temperature, topP, topK, repeatPenalty, callback)` | streams tokens via the callback |
| `nativeFree(handle)` | releases the session |

## Notes
- Ship only `arm64-v8a` (and optionally `x86_64`) to keep the APK small; models are downloaded at
  runtime, never bundled.
- Prompts use the Qwen2.5 ChatML format (`<|im_start|>` / `<|im_end|>`); stop generation on the
  `<|im_end|>` token as well as EOS.
