# Test plan

Two halves: what CI proves automatically, and what only a human with a stylus can judge. The manual
half is not optional — recognition accuracy and whether synthesized handwriting *looks* like the
user's are not assertable properties.

## Automated

```bash
# Everything except the native GGUF backend (no NDK needed).
./gradlew -Painotebook.native.llama=false testDebugUnitTest

# Single module while iterating.
./gradlew -Painotebook.native.llama=false :domain:test

# Room migration tests (needs a device or emulator).
./gradlew -Painotebook.native.llama=false :data:connectedDebugAndroidTest

# Lint.
./gradlew -Painotebook.native.llama=false lintDebug
```

| Area | Module | What it pins down |
|---|---|---|
| Stroke segmentation | `:domain` | Reading order; every input stroke lands in exactly one segment; gap thresholds scale with x-height, not pixels |
| Math token repair | `:domain` | `x`→`×` only between digits; `l`→`1`, `O`→`0`, `S`→`5` in numeric context; implicit multiplication |
| Candidate reranking | `:domain` | A parseable lower-scored candidate beats an unparseable higher-scored one |
| Offline math engine | `:domain` | Exact rationals (`1/3 + 1/6 = 1/2`), precedence, unary minus, quadratics, derivatives, and that unsolvable input returns `null` rather than a guess |
| Complexity routing | `:domain` | Math → offline; non-math → cloud when a key exists; low-RAM device refuses heavy local work |
| Handwriting synthesis | `:domain` | Determinism under a fixed seed; word-boundary wrapping; `measure` agrees with `synthesize`; missing glyphs group into one fallback run |
| Glyph normalization | `:domain` | Normalization against box guides, not ink bounds; advance widths; slant estimation |
| Cloud failover | `:data` | 429 → next provider; 401 → stop, no rotation; 5xx → one retry then rotate; **no failover after the first token**; SSE frames split across reads |
| Solve orchestration | `:data` | Emission **order** for cache hit / offline-math / cloud / fallback; `allowCloud = false` never reaches a cloud client |
| Backend selection | `:data` | `.task` → MediaPipe, `.gguf` → llama.cpp; big-core clustering from fixture frequency lists |
| Room migration | `:data` | v2 → v3 preserves existing notebooks and installed models |

Emission order is asserted, not just terminal state — with this design the order *is* the behaviour.

## Manual

Do these on a real low-end device (4 GB RAM) and, if possible, a 6 GB+ device. The emulator will
mislead you on both inference speed and stylus input.

### 1. Handwriting training

1. Fresh install → the welcome screen states the privacy promise.
2. Take the "Train now" path. In the number stage, confirm every cell shows **printed baseline and
   x-height guides** — normalization depends on them, and their absence is the difference between
   natural output and a ransom note.
3. Write `0`–`9`. Verify undo-last-stroke, clear, and one-tap re-record.
4. Leave a cell empty and advance → rejected with a specific prompt, not silently accepted.
5. Kill the app mid-stage, reopen → progress preserved, resumes where you left off.
6. Finish a stage → the review screen renders a sample sentence in *your* hand. Judge it honestly:
   are letters sitting on a common baseline, are proportions right (`h` taller than `x`, `p`
   descending), is spacing plausible?
7. Re-record one letter you dislike → the sample sentence changes accordingly.
8. Take the "Skip" path on a second install → app fully usable, answers render as typed text.

### 2. Recognition (the thing that was broken)

1. First recognition triggers a model download → the wait is explained, not a silent hang.
2. Write, in your normal hand: `12 + 47 =`, `3x + 5 = 20`, `√144`, `2/3 + 1/6`, and a word problem
   in prose. Select each and solve.
3. Confirm the recognized reading is correct **before** the answer. This is the fix: previously
   strokes were rasterized and fed to a printed-text OCR model.
4. Check the confidence heatmap marks genuinely ambiguous words and not everything.
5. Tap a low-confidence word → alternatives appear → pick one → the answer re-computes.
6. Hand-edit a word, then trigger recognition again → your edit is **not** overwritten.
7. Write two lines of prose and confirm line order and word spacing are right — line/word
   segmentation is what makes this work.
8. Write cramped math with an i-dot, a `t`-cross, an exponent and a division bar → confirm those
   strokes attach to their neighbour instead of becoming separate "words".
9. Zoom to 25% and to 400%, write, and recognize → accuracy must not change, because gap thresholds
   are relative to x-height rather than pixels.

### 3. Bring your own API key

1. Settings → AI & Providers on a fresh install → empty state makes adding a key the obvious step.
2. Add a provider from the **Groq** preset: paste a key, tap **Test connection** → latency reported.
3. Enter a deliberately wrong key → the provider's actual error surfaces ("invalid api key"), not
   "HTTP 401".
4. Enter a bad model id → the real message surfaces, and **no key rotation occurs** (this is the
   permanent-vs-transient distinction; rotating here would burn every key you own).
5. Repeat for Gemini and Anthropic presets, then for **Other / self-hosted** against a local Ollama
   or LM Studio `/chat/completions` endpoint. All three protocols must stream.
6. Add two providers, disable the first → the second serves. Reorder → order is honoured.
7. Force a 429 (hammer a free tier) → failover to the second provider, and the badge names which
   one answered.
8. Solve a non-math question with cloud **off** → content does not leave the device; the app offers
   escalation rather than silently sending.
9. Tap escalate once → it sends, and the global cloud setting stays off afterwards.
10. Kill and reopen the app → keys persist. Delete a provider → its key is gone from the keystore.
11. Airplane mode → clear failure with a route out, never a hang.

### 4. Local model speed (the slowness complaint)

1. With no model installed, everything math still answers instantly — no download required.
2. Model Manager → download the recommended MediaPipe `.task` model. Note the size warning matches
   reality.
3. Ask a non-math question. Measure **time to first token**, then tokens/second. Then ask a
   follow-up in the same session and measure again: the second must be markedly faster, because the
   session is reused rather than re-prefilling the prompt.
4. Turn on a GPU-less/old device or force CPU → confirm it degrades to CPU rather than failing.
5. Background the app during generation, return → no crash, no orphaned session.
6. Cancel mid-generation → generation actually stops (watch CPU), partial text retained.
7. Delete the model → space is freed and the figure shown was honest.
8. **If you build the NDK**, repeat 3–7 with a GGUF model, and confirm in Logcat (`ainotebook_llama`)
   that the prompt cache reports reusing a prefix on the follow-up turn. Compare against a build with
   the ARM arch flags removed to see what they are worth — this was the main cause of the original
   slowness.

### 5. Answers in your handwriting

1. Solve something with training complete → tap **Write on page**.
2. The answer writes on progressively with the ink-ripple, placed near the question without
   overlapping existing ink.
3. Erase part of the answer, and undo → the answer is removed **as one unit**, not stroke by stroke.
4. Export to PNG and PDF → the handwritten answer appears, identical to on screen.
5. Reopen the page → the answer re-renders identically (synthesis is seeded, so it must not reflow).
6. Ask something containing a symbol you never trained → it renders as typed text **with a visible
   badge**, and the rest stays handwritten.
7. Clear handwriting data in Settings → answers fall back to typed text without errors.

### 6. Low-memory behaviour

1. On a 4 GB device, confirm the low-memory notice appears and lighter defaults are pre-selected.
2. Enable Fast/Low-Mem mode → local model disabled, offline math plus cloud only, and the UI says so.
3. Open ~20 pages with heavy ink, keep drawing → watch for jank and for a rising heap. Stroke
   rendering must stay smooth; per-frame allocation in the draw loop is the usual culprit.
4. Run inference while drawing → the UI must stay responsive, which is why thread count targets big
   cores only rather than all cores.

### 7. Privacy claims

Every claim in `docs/PRIVACY_POLICY.md` should be verifiable. Spot-check the load-bearing ones:

1. Proxy the device (mitmproxy/Charles) and exercise the whole app with cloud **off** → the only
   traffic is model/catalogue downloads. No note content.
2. Turn cloud on and solve → the request body contains the recognized text **only**. Confirm it
   contains no stroke data, no glyph samples, and no page image.
3. Search the request for anything resembling handwriting samples → there must be none, on any
   setting.
4. Export an `.ainb` → confirm it contains no API keys and no response cache.
5. `adb shell run-as` the app sandbox → the stored key is ciphertext, not readable text.

## Known gaps

- **Native GGUF backend is unverified by compilation here.** The C++ and CMake changes are written
  against the vendored llama.cpp headers but were never compiled in this environment. Build it once
  in Android Studio with the NDK installed before relying on that path; the MediaPipe backend is the
  default precisely so this is not on the critical path.
- Recognition accuracy is only measurable against real handwriting. Treat step 2 above as the
  acceptance gate.
- Model download URLs that sit behind a license click-through cannot be fetched programmatically;
  `config/README.md` says which ones.
