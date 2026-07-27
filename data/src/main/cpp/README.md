# On-device inference (llama.cpp) — native build

This directory holds the JNI bridge that lets `LlamaInferenceEngine` run your GGUF models
(e.g. Qwen2.5) fully on-device with **no network access**.

- [`ainotebook_llama.cpp`](ainotebook_llama.cpp) — the JNI implementation (load, generate, free).
- [`CMakeLists.txt`](CMakeLists.txt) — builds `libainotebook_llama.so` and links llama.cpp.

The native build is **off by default** and turns on automatically once you vendor llama.cpp into
`data/src/main/cpp/llama.cpp` (see `data/build.gradle.kts` — `llamaNativeEnabled`). Until then the
app builds and runs normally, and the AI panel shows *"On-device inference is not available in this
build."* Once the `.so` is built, that message disappears and generation works.

---

## One-time setup (do this in Android Studio on your machine — I can't compile here)

### 1. Install the NDK + CMake

Android Studio → **Settings → Languages & Frameworks → Android SDK → SDK Tools**, tick:

- **NDK (Side by side)** (r26 or newer)
- **CMake** (3.22.1 or newer)

### 2. Vendor llama.cpp

From the `AI-Notebook/` project root, add llama.cpp as a git submodule (or just clone it) so it
lands at exactly `data/src/main/cpp/llama.cpp`:

```bash
git submodule add https://github.com/ggml-org/llama.cpp data/src/main/cpp/llama.cpp
cd data/src/main/cpp/llama.cpp
git checkout master        # or pin a tag/commit you trust
git submodule update --init --recursive
```

> No git? Download the llama.cpp source zip and extract it so that
> `data/src/main/cpp/llama.cpp/CMakeLists.txt` exists.

### 3. Sync + build

Back in Android Studio: **File → Sync Project with Gradle Files**, then **Build → Make Project**
(or run the app). Gradle now sees the `llama.cpp` folder, enables `externalNativeBuild`, and CMake
compiles `libainotebook_llama.so` for `arm64-v8a` and `x86_64`. First build is slow (llama.cpp is
large); later builds are cached.

### 4. Verify

Run on a device, open a notebook, trigger AI. In **Logcat** filter by tag `ainotebook_llama` — you
should see `Model loaded (n_ctx=..., threads=...)`. Tokens then stream into the AI panel.

---

## API compatibility notes

`ainotebook_llama.cpp` targets the **current (2025) llama.cpp API**. If you pin an older commit and
the build fails with "undeclared identifier", map these renamed symbols to whatever your commit
uses:

| Used here (current)              | Older name                          |
| -------------------------------- | ----------------------------------- |
| `llama_model_load_from_file`     | `llama_load_model_from_file`        |
| `llama_init_from_model`          | `llama_new_context_with_model`      |
| `llama_model_get_vocab` + vocab  | pass `model` directly               |
| `llama_tokenize(vocab, …)`       | `llama_tokenize(model, …)`          |
| `llama_token_to_piece(vocab, …)` | `llama_token_to_piece(model, …)`    |
| `llama_vocab_is_eog(vocab, tok)` | `llama_token_is_eog(model, tok)`    |
| `llama_model_free`               | `llama_free_model`                  |
| sampler chain (`llama_sampler_*`)| `llama_sample_token_*` + candidates |

The safest path is to check out llama.cpp `master` so the code matches as-written.

---

## Contract (must match `LlamaInferenceEngine`)

| Kotlin `external fun` | JNI symbol |
|---|---|
| `nativeLoadModel(path, ctxLen, threads, gpuLayers): Long` | returns an opaque session handle, or `0` on failure |
| `nativeGenerate(handle, prompt, maxTokens, temperature, topP, topK, repeatPenalty, callback)` | streams tokens via the callback |
| `nativeFree(handle)` | releases the session |

Prompts use the Qwen2.5 ChatML format (`<|im_start|>` / `<|im_end|>`). The generation loop stops on
any end-of-generation token via `llama_vocab_is_eog`, which for Qwen covers both EOS and `<|im_end|>`.

## Build options

`CMakeLists.txt` disables llamafile, OpenMP, curl, tests, examples, tools, and the server to keep
the library lean. ABIs are limited to `arm64-v8a` (all modern phones) and `x86_64` (emulator) in
`data/build.gradle.kts`. Devices with other ABIs simply fall back to "AI unavailable" — the app
still runs. Models are downloaded at runtime, never bundled in the APK.

## How it fits together

```
AiEngineImpl → LlamaInferenceEngine (Kotlin, JNI declarations)
                    │  System.loadLibrary("ainotebook_llama")
                    ▼
            libainotebook_llama.so  ←— ainotebook_llama.cpp (this dir)
                    │  links
                    ▼
                 llama.cpp  ←— your GGUF model file on disk
```

`nativeGenerate` streams each token back through the `TokenCallback.onToken(String): Boolean`
interface; returning `false` (e.g. the user cancels) stops generation cleanly.
