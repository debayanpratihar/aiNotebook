# Hosted config (`ai-notebook-config`)

The app reads its model catalog from:

```
https://debayanpratihar.github.io/ai-notebook-config/
  ├─ config.json       ← app-wide settings + default model
  ├─ models.json       ← the downloadable model catalog (this is what shows in Model Manager)
  ├─ announcements.json
  └─ changelog.json
```

`config.json` and `models.json` in this folder are ready to upload to that GitHub Pages repo. They
replace the Qwen-only catalog with a **RAM-tiered** set of small models.

## Tiers (by device RAM)

| id         | model              | ~size  | min RAM | who it's for                                   |
| ---------- | ------------------ | ------ | ------- | ---------------------------------------------- |
| `compact`  | Qwen2.5 0.5B       | ~400MB | 2 GB    | low-end phones; default recommended            |
| `balanced` | Llama 3.2 1B       | ~800MB | 3 GB    | mid-range; better word-problem reasoning       |
| `high`     | Qwen2.5 1.5B       | ~1.1GB | 4 GB    | high-end; best quality                          |

- **1 GB RAM phones**: no model is offered (all require ≥2 GB). They still get the full **offline math
  engine** — arithmetic, equations, and basic calculus are solved instantly on-device with no model.
- The `id` value drives the tier shown in the app (`compact` → Compact, `high` → High quality,
  anything else → Balanced), so keep these ids.
- A high-end phone can still choose `compact` — every compatible tier is selectable.

## ⚠️ Verify the download URLs before shipping

I set the Hugging Face URLs to the standard `resolve/main/<file>` form, but I could not open them from
here. **Confirm each one downloads** before relying on it:

1. Open the `repo` page on huggingface.co (e.g. `Qwen/Qwen2.5-0.5B-Instruct-GGUF`).
2. Open the **Files** tab, click the `.gguf` in `filename`.
3. Use the file's **download** link as `downloadUrl`. If Hugging Face renamed the file, update
   `filename` + `downloadUrl` to match.

`sha256` is left blank on purpose — the app skips the integrity check when it's blank, so downloads
work immediately. To harden a release, paste each file's real SHA-256 (shown on the HF file page).

## How to publish

```bash
git clone https://github.com/debayanpratihar/ai-notebook-config
cd ai-notebook-config
cp <this-repo>/AI-Notebook/config/models.json .
cp <this-repo>/AI-Notebook/config/config.json .
git commit -am "Tiered model catalog"
git push
```

GitHub Pages serves the update within a minute. In the app, **Settings → Models** → pull to refresh.
