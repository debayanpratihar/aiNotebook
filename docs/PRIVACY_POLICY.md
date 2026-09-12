# Privacy Policy — AI Notebook

**Last updated:** 13 September 2026
**Applies to:** AI Notebook for Android (`com.debayan.ainotebook`)

> Template. Before publishing, replace the contact address below, confirm every statement still
> matches the shipped build, and have it reviewed if you operate in a jurisdiction with specific
> disclosure requirements. Inaccurate privacy disclosures are a common cause of Play Store
> rejection and of data-safety-form mismatches.

## The short version

AI Notebook is offline-first. Your notes, your ink, and your handwriting stay on your device. The
app has no account, no analytics, and no telemetry. Nothing is uploaded unless you explicitly turn
on cloud AI, and even then only the *recognized text* of the problem you asked about is sent — to
the provider whose key you supplied, and nowhere else.

## What stays on your device, always

| Data | Where it lives | Ever leaves the device? |
|---|---|---|
| Notebooks, pages, strokes (your ink) | App database, app-private storage | No |
| Handwriting samples from training | App database, app-private storage | **No — never, under any setting** |
| Recognized text and the search index | App database | Only as described under "Cloud AI" |
| API keys you enter | Encrypted with a key held in the Android Keystore | Only to that provider, as the request's auth header |
| AI answer cache | App database | No |
| Preferences | DataStore, app-private storage | No |

Handwriting samples deserve a specific commitment: a handwriting model is an identifier closely
tied to you as a person, and solving a maths problem never requires it. It is excluded from cloud
requests entirely, and it is excluded from the crash-report path because there is no crash-report
path.

## Handwriting recognition

Recognition runs **on your device** using Google ML Kit Digital Ink Recognition. The first time you
use it, ML Kit downloads a recognition model for your language from Google's servers; that download
requests a model file and does not send your ink. Recognition itself is local: your strokes are
never transmitted for recognition.

The same applies to the printed-text recognition used when you import a photo of a printed page.

## Maths

Calculations, equations, and the step-by-step working are computed by an engine built into the app.
No network, no model, no data leaving the device. This is the path most problems take.

## On-device AI models (optional)

If you choose to download a language model, it is fetched over HTTPS from the model host shown in
the Model Manager (for example Hugging Face or Google). The download requests a file; it does not
send your notes. Once installed, that model runs entirely on your device and needs no network.

You can delete a downloaded model at any time in Settings → AI & Providers or the Model Manager,
which frees the space and stops it being used.

## Cloud AI (optional, off by default)

Cloud AI is **opt-in**. It is off until you both add a provider and enable it, and you are told what
will be sent before the first request.

When you use it:

- **What is sent:** the recognized text of the problem, and — if you asked a question about your
  notes — the text of the relevant part of the page. For maths, a compact normalized expression is
  sent rather than the surrounding page.
- **What is never sent:** your ink or stroke data, your handwriting samples, page images, other
  notebooks, file names, your contacts, your location, or any device identifier.
- **Where it goes:** only to the provider you configured, at the base URL you entered, authenticated
  with the key you supplied. AI Notebook operates no server of its own and receives no copy.
- **What they do with it:** governed by *their* privacy policy and terms, not this one. Providers
  differ substantially on retention and on whether inputs may be used for training. If that matters
  to you, read the policy of the provider you choose before enabling cloud AI.

Answers are cached locally so that repeating a question is instant and costs nothing. Clearing the
cache (Settings → Storage & Privacy) deletes them. Cached answers remain readable after you disable
cloud AI; they are not re-fetched.

If the build you installed shipped with a built-in free-tier provider, it is listed in AI &
Providers like any other and can be disabled or deleted there.

## Your API keys

Keys are encrypted with AES-GCM using a key generated in, and non-exportable from, the Android
Keystore, and the ciphertext is stored in app-private storage. Keys are not written to logs, are not
included in exports or backups of your notebooks, and are deleted from the keystore when you delete
the provider.

A caveat worth stating plainly: no client-side storage can protect a secret from someone with
physical access to an unlocked, rooted device. Use a key scoped to what you are willing to risk, and
revoke it in your provider's console if you lose the device.

## What the app does not do

- No account, sign-in, or user profile.
- No analytics, crash reporting, advertising, or attribution SDKs.
- No tracking across apps or websites; no advertising ID access.
- No selling or sharing of personal information.
- No background upload of any kind.

## Permissions

- **Internet** — only for: downloading a recognition model, downloading a language model you chose,
  fetching the model catalogue, and cloud AI requests you enabled.
- **Storage access** (scoped, via the system picker) — only when you export or import a file.

The app requests no camera, microphone, contacts, or location permission.

## Children

AI Notebook is suitable for students and contains no ads and no social features. It collects no
personal information, so there is nothing to collect from a child either. If you enable cloud AI on
a child's device, that provider's own policy and age terms apply to what is sent.

## Retention and deletion

Everything is local, so you control retention completely:

- Delete a notebook, page, or stroke — removed from the database.
- Settings → Storage & Privacy → **Delete handwriting data** — removes every recorded sample.
- Settings → Storage & Privacy → **Clear answer cache** — removes cached AI answers.
- Settings → AI & Providers → delete a provider — removes its key from the keystore.
- Uninstalling the app removes all of it.

## Your rights

Because the app holds no data about you on any server, there is no account to access, export, or
erase. Your data is already in your possession: export notebooks from the app, and delete them with
the controls above. If you are in a jurisdiction granting rights over personal data (GDPR, CCPA and
similar), those rights are satisfied by that local control; there is no server-side copy to request.

## Changes

Material changes to this policy will be published with the app update that introduces them, with the
date above updated. If a future version changes what is sent off the device, that will require your
consent again rather than being enabled silently.

## Contact

**REPLACE-WITH-YOUR-CONTACT-EMAIL**
