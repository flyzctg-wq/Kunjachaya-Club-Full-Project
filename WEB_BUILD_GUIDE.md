# Web Build Guide — Kunjachaya Club (React + Vite)

Focused, step-by-step guide to building and deploying `web/`. For the wider project (Android, Firebase project setup, Cloud Functions), see [BUILD.md](./BUILD.md).

---

## 0. What you need before running anything

| Requirement | Why |
|---|---|
| Node.js 18+ (20 recommended) | Vite 5 requires it; CI uses Node 20 |
| npm | Comes with Node |
| Nothing else | No `.env` file, no environment variables, no API keys to configure locally — see §2 |

You do **not** need the Firebase CLI installed just to build the frontend — only for deploying Firestore rules/Functions or using Firebase Hosting instead of Vercel.

---

## 1. Install & run locally

```bash
cd web
npm install
npm run dev
```

Opens a local dev server (Vite's default, typically `http://localhost:5173`) with hot module reload.

---

## 2. Configuration — there's nothing to set up

Unlike most projects, there's no `.env` step here. `web/src/firebase.js` already contains the real, live Firebase web config for the `kunjachaya-club` project, hardcoded directly:

```js
const firebaseConfig = {
  apiKey: "AIzaSyBt1CUvopFzQC6STqQ-lJ1R9GG5dvS-sXI",
  authDomain: "kunjachaya-club.firebaseapp.com",
  projectId: "kunjachaya-club",
  // ...
};
```

This is intentional and safe: a Firebase **web** config is an identifier, not a secret — it tells the browser SDK which project to talk to. It carries no special privilege by itself. Real access control lives entirely in `firestore.rules` (deployed separately — see BUILD.md §2) and in the Cloud Function's server-side PipraPay credentials (BUILD.md §3), neither of which this repo exposes to the client.

So: `npm install && npm run build` works immediately, with no setup step, on any machine.

---

## 3. Production build

```bash
npm run build
```

Outputs static assets to `web/dist`. Preview the production build locally before deploying:

```bash
npm run preview
```

---

## 4. Deploy

### Vercel (recommended)
From the **repo root** (not `web/`) — `vercel.json` at the root already tells Vercel to build only the `web/` subdirectory, since this is a monorepo that also contains the Android app and Cloud Functions:

```bash
npm install -g vercel
vercel --prod
```

Or connect the GitHub repo directly in the Vercel dashboard — no configuration needed beyond what's already in `vercel.json`. No environment variables to set (see §2).

### Firebase Hosting (alternative)
```bash
npm --prefix web run build
firebase deploy --only hosting
```
(`firebase.json` at the repo root is already configured to serve `web/dist` with SPA rewrites.)

---

## 5. What was verified via static checks

This environment has no network access, so `npm install` has never actually been run here — everything below was checked by reading the source directly, not by a real build:

- **Every non-relative import matches a declared dependency.** Cross-referenced every `import ... from '<package>'` across all 21 `.js`/`.jsx` files against `package.json` — `firebase`, `jspdf`, `react`, `react-dom` are the only external packages actually used, and all four are declared. No missing dependency.
- **Every relative import (`./`, `../`) resolves to a real file.** Checked separately — no broken local imports (a stale import path is a common cause of a build failing after a file gets renamed or deleted).
- **Brace/paren/bracket balance** across all files — clean.
- **Config file format consistency** — `package.json` declares `"type": "module"`, and `vite.config.js`, `postcss.config.js`, `tailwind.config.js` all correctly use ESM (`export default`) rather than CommonJS (`module.exports`), which would otherwise throw at build time.

None of this replaces an actual `npm install && npm run build` — it only rules out the class of errors that show up as a broken import or malformed config before you even get that far.

---

## 6. Known non-blocking gap: `npm run lint`

`package.json` declares a `"lint": "eslint ."` script, but `eslint` was never added as a dependency, and there's no ESLint config file in `web/`. Running `npm run lint` will fail immediately (`eslint: command not found`) — this is a pre-existing gap, not something introduced later, and it does **not** affect `npm run build` or `npm run dev`, since neither calls lint. If you want linting:

```bash
npm install -D eslint eslint-plugin-react eslint-plugin-react-hooks
```
then add an `eslint.config.js` (ESLint 9+ flat config) or `.eslintrc.json` (older config format) — whichever matches whatever ESLint version you install.

---

## 7. Troubleshooting

- **Blank page after deploy, console shows a Firebase error** → almost always means Firestore rules or Cloud Functions haven't been deployed yet (BUILD.md §2–3), not a problem with the web build itself.
- **"Missing or insufficient permissions" from Firestore** → expected if you're testing before `firestore.rules` has been deployed, or if the signed-in account hasn't been given a real `memberClass`/`committeePost` yet (new accounts start `Pending`).
- **PipraPay checkout button does nothing / throws `failed-precondition`** → the Cloud Function is intentionally rejecting the request because real PipraPay merchant credentials aren't configured yet (BUILD.md §3) — this is the correct failure mode, not a bug.
- **Styles look unstyled / Tailwind classes not applying** → check `tailwind.config.js`'s `content` globs still match your file locations if you move files around; Vite won't error on this, it'll just silently produce a smaller-than-expected CSS bundle.
- **`npm install` fails on a fresh clone** → check your Node version (`node -v`) against §0; very old Node versions aren't supported by Vite 5.
