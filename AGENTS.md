# Nougat for Android: instructions for any coding agent

This is the Android version of Nougat, a free music player for the files people already own. Varun owns it. The iPhone version is finished and is the reference for how everything should behave. Claude sessions (and possibly ChatGPT) take turns here on Varun's Mac; the next one knows only what is written in this folder.

| File | What it is | Who edits it |
|---|---|---|
| `HANDOFF.md` | Where things stand, open questions, decisions, session log | Every agent, every session |
| `PLAN.md` | Ordered tasks with "Done when" checks | Tick boxes; change tasks only with Varun's agreement |
| `DESIGN.md` | How the Android design differs from the iPhone one | Only when a rule is missing; note it in the handoff |

## The iPhone version is the reference

It lives at `/Users/varun/work/NougatMobileApp` (public copy: https://github.com/vsvanshi/nougat_mobile_app).

- **Read only.** Never edit, build into, commit to or add files to that folder from here. Its rules forbid any mention of Android in it, so do not add one.
- Its `DESIGN.md` holds the shared design tokens (colours, accents, type, layout, motion). Its `HANDOFF.md` "Decisions" table explains why things behave as they do. Its Swift code is the reference implementation: when porting a feature, read the matching Swift file and its tests first (PLAN.md names them).
- Behaviour should match the iPhone version unless `DESIGN.md` or a decision here says Android does it differently.

## Start of every session

1. Read `HANDOFF.md` from top to bottom.
2. If the last session left anything broken or unverified, deal with that first.
3. Take the task `HANDOFF.md` names as next, or the first unticked one in `PLAN.md`.
4. Read the parts of `DESIGN.md` here, and of the iPhone `DESIGN.md`, that the task touches.

## While working

- One task at a time, in plan order. Smallest change that meets the task's "Done when".
- Decisions in `HANDOFF.md` are settled. If you think one is wrong, add it to Open questions and carry on.
- Material for everything, drawn with the design tokens. If a colour, size or style is missing, add it to `DESIGN.md` in the same change and say so in the handoff.
- No dependencies outside AndroidX / Jetpack (Compose, Media3, Lifecycle and the like) without Varun's approval.
- Do not download or install anything (Android Studio, SDK packages, emulator images, Gradle distributions the first time) and do not change anything in Varun's accounts without asking him first.
- Keep device identifiers, keystores, signing passwords and other private details out of the repository; it may become public.

## Working with Varun

- He writes in English or Hinglish; answer in whichever he used.
- He tests on real devices and reports back. Install a build after every change he should look at.
- He wants no duplicates anywhere: songs, folders, playlists, songs within a playlist.
- He likes being asked before anything risky, and short, plain explanations.

## Git

- Commit only when Varun says to. Push only when he says so.
- Commit messages are short and plain, the way a person would write them: `Add library scanner`, `Fix seek after pause`.
- No co-author lines and no AI attribution.

## Verifying

Commands are in `PLAN.md`. A task is done only when you have checked its "Done when" yourself. If you could not build, run or see the result, say so plainly in the handoff and leave the box unticked; the next session verifies it before starting anything new.

## End of every session, finished or not

1. Update "Current state" in `HANDOFF.md`.
2. Add a session entry at the top of the session log.
3. Tick only the boxes you verified.
