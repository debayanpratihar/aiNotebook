# Play Store Listing — AI Notebook

## Title
AI Notebook — Offline Handwriting + AI

## Short description (≤ 80 chars)
Handwrite, draw, and get on-device AI help — fully offline and private.

## Full description
AI Notebook is an offline-first handwriting notebook with an on-device AI writing partner.
Write and draw on an infinite canvas, organize notebooks, and let a local AI model help — all
without your notes ever leaving your device.

- Infinite canvas: smooth vector ink, pen/pencil/marker/highlighter, colors, eraser, undo/redo, zoom & pan.
- On-device AI: download a local model and generate directly in your notebook. No account, no cloud.
- Offline handwriting search: on-device OCR indexes your notes so you can find them instantly.
- Export & share: lossless native package (.ainb), PDF, and PNG/JPEG.
- Private by design: notebooks, handwriting, OCR text, and AI prompts stay on the device.

## Model download explanation (required — the app downloads AI models)
The app ships without an AI model to keep the download small. On first use you choose and download
a local model (e.g. Qwen2.5 Instruct, ~0.9–4.3 GB) from Hugging Face over HTTPS. The model is stored
on the device and runs entirely offline. Downloads default to **Wi-Fi only** and can be paused/resumed;
files are size- and (when provided) SHA-256-verified. You can delete models anytime in Model Manager.

## Privacy policy (summary — host the full text publicly and link it)
- No notebook content, handwriting, OCR text, or AI prompts are transmitted off-device.
- Network is used only to: download AI models, fetch the model configuration, and honor user-initiated
  share/export.
- No analytics or crash reporting without explicit consent.
- Users can delete notebooks and models at any time.

## Accessibility disclosure
- Material 3 with light/dark and dynamic color; content-description labels on actions.
- Large touch targets; stylus and finger input; adjustable stroke smoothing and pressure.
- Planned: high-contrast mode, left-handed mode, reduced motion (Settings → Accessibility).

## Testing instructions (for review)
1. Launch → tap **New notebook** → draw with the pen; switch colors/tools; undo/redo; pinch-zoom.
2. Rename via the notebook title or the ⋮ menu; delete from the Home ⋮ menu.
3. **⋮ → Model Manager** → download a model on Wi-Fi → activate it. (Generation output requires the
   native inference library in this build; the pipeline and UI are otherwise functional.)
4. Open a notebook → **Export** → PDF/PNG/.ainb → **Share**. Then **⋮ → Import notebook** to restore.
5. **⋮ → Settings** — toggle theme; confirm it persists after relaunch.

## Assets to prepare before submission
- App icon (adaptive; already in-app), 512×512 hi-res icon, 1024×500 feature graphic.
- Phone + 7"/10" tablet screenshots: Home, canvas drawing, Model Manager, Settings, Export.
