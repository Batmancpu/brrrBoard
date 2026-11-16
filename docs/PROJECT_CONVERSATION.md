Warning: truncated output (original token count: 26619)
Total output lines: 556

# FrostKeys / brrrBoard conversation record

## 2026-11-14

This record covers the project conversation visible at handoff. User messages below are quoted verbatim, including spelling and punctuation. Assistant outcomes and corrections are recorded alongside them, with the exact release and update instructions retained below. The entries link to public project evidence where available. Earlier chat transcripts were unavailable. Raw tool output, credentials, and private material are excluded.

### Repository audit and persistent handoff

**User**

> Acces my frost keys repo and check what was i trying to do and what happened Also, I wanted to create an context file in the repository that whatever work we are doing, every single small detail changes, our plan, our goal, our changes in our plan, our changes in our goal, everything should be there in somewhere so that if I change AI agents or start a new chat, the context is picked on by AI and it can start right where it ended. It's like I'm talking to the same person

**Assistant outcome.** Identified `Batmancpu/brrrBoard` as the user's FrostKeys fork. Audited `main`, the Jules branch, PRs #1–#6, and APK delivery. The last October main build produced a signed artifact, but the emoji assertion failed and publication was skipped. Added `AGENTS.md` and `PROJECT_CONTEXT.md` on `main` in [`d8e37f4`](https://github.com/Batmancpu/brrrBoard/commit/d8e37f426287194002c7665ab2654856d07f786e).

### Stable APK updates and Obtainium

**User**

> So dude, I want you to set up a signing key. Okay, do this thing. Set up a signing key so that what I can do is I can just install this application in my phone through Optaneum and I can update it easily. No need to go to Chrome, download, installation and just go to Octaneum refresh and update

**Assistant outcome.** Added a separate app package, private signing workflow and enrollment script on `main` in [`082299d`](https://github.com/Batmancpu/brrrBoard/commit/082299d9bc158f6aab720791f2dc9ee9472a90fb). Older test packages had different identities. The existing debug key is public. Initial workflow run [43582729776](https://github.com/Batmancpu/brrrBoard/actions/runs/43582729776) stopped at signing enrollment. The assistant initially gave the owner a terminal setup command.

**User**

> Do all the work for me and just give me the direct link of the page and small manual tasks that need to be done for me. And I'll do that

**Assistant outcome.** Attempted an authenticated browser handoff because the available repository connection could write code but did not offer an Actions secrets write operation. Left a GitHub sign-in link.

**User**

> I am unable to sign into GitHub. So just give me the link of the page. I'll open it in Safari and do the work that you are asking me to. I'll do the secret pasting thing

**Assistant outcome.** Gave the direct [Actions settings](https://github.com/Batmancpu/brrrBoard/settings/secrets/actions) and [release workflow](https://github.com/Batmancpu/brrrBoard/actions/workflows/obtainium-release.yml) links. Asked the owner to add the keystore and password as secrets and the certificate fingerprint as a variable. The owner still needed values to paste.

**User**

> Why don't you run this fucking script and give me the value so that I just have to copy paste? I have given the name Optanium keystore base 64. Now give me the values to be put in secrets for both the base64 and password.

**Assistant outcome.** Generated one release key, checked that the Base64 decoded to the original keystore, and provided the two secret values and certificate fingerprint in a downloadable file. Specified exact GitHub field names. No private values belong in this repository.

**User**

> This signing values TXT I click on it and it says file not found.

**Assistant outcome.** Recovered the original file and supplied a working link to the same values. The key was preserved.

### First install and physical-device update test

**User**

> I created all the three secrets with the values pasted below them. So let's go and first check this update over the existing app workflow with the first app. And then after I say first app is installed, you give me a second app. Or why don't you just run the first app and then the second app after two minutes?

**Assistant outcome.** Chose to wait for installation confirmation so the phone could test a real update. The retry showed that the workflow expected the fingerprint under Variables. Updated it to also accept the fingerprint from Secrets ([`2b0dc4e`](https://github.com/Batmancpu/brrrBoard/commit/2b0dc4e16b10f962708b564e0eaea582385b0d99)). The next run compiled the APK and failed signature verification. Added separate keystore certificate verification and support for both observed apksigner digest prefixes ([`1562bae`](https://github.com/Batmancpu/brrrBoard/commit/1562bae56f842258b0bf620c5b521bd17269780d)). The next run verified and published [v1.0.3](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.3), package `com.orion.frostkeys.brrrboard`, version code 3. The assistant held the second build until the owner confirmed installation.

**User**

> Hey so my 1. 0.3 is installed. My Optaneium limit for today is done, so I'm using the Chrome to download and install it

**Assistant outcome.** Started a new run from `main` and published [v1.0.4](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.4). CI verified the same package and signer with version code 4. Only the handoff and a workflow comment differed from v1.0.3. Sent the direct APK link and asked the owner to update over v1.0.3 and check the version and settings.

**User**

> तो old update pipeline has worked. Now do you know my ultimate goal with this? Because I was thinking of getting, giving it to, you know, Codex. So I want you to create a context page inside the repository that if I give it to any AI agent, change agents, then it should know each and every point verbatim, all the chat, all the changes, all my goal, changes to goal, changes to chat and everything. It's like, it's like I have transferred the memory from one person's brain to another person's brain. It should be that. Okay? So if I change chats or change AI agents and then also do you know my goal and everything with this? Because I am thinking of handing over this to ChatGPT Codex

**Handoff outcome.** Recorded the owner-reported Chrome update as successful. Settings retention was not separately reported. Obtainium discovery and update on the phone are still unverified. Expanded the handoff and its maintenance instructions. The owner has confirmed the release and continuity goals. The historical keyboard direction is supported by repository evidence; final feature priorities and acceptance criteria remain open.

## Verbatim assistant instructions retained at handoff

These quotes preserve the build and install instructions that the owner acted on. The chronological record above retains earlier assistant outcomes and corrections. Private attachment links and account-specific sign-in links are omitted.

### First signed release

> The first signed APK, **brrrBoard v1.0.3**, is live. GitHub verified its signing certificate, package ID, and version code. [Release page](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.3)
>
> 1. In Obtainium, tap **Add App** and paste `https://github.com/Batmancpu/brrrBoard`.
> 2. Set **Filter Release Titles by Regular Expression** to `^brrrBoard Obtainium`.
> 3. Tap **Install** and confirm it shows **1.0.3**.
>
> This is a fresh install alongside any older FrostKeys or brrrBoard APK. **Tell me once 1.0.3 is installed.** I’ve held back the second build so you can test **Refresh → Update** over that exact install.

### In-place update test

> [Download brrrBoard 1.0.4 APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.4/brrrBoard.apk)
>
> Open the APK from Chrome and tap **Update** without uninstalling 1.0.3. GitHub verified that 1.0.4 uses the same package ID and signing certificate, with a higher version code. This build contains no app feature changes. [Release page](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.4)
>
> Tell me whether it shows **1.0.4** afterward and your keyboard settings are still there. That confirms the in-place update on your phone.

## Add the next exchange

Copy the exact project-relevant user message, preserving language and order. For new entries also retain the assistant's project-relevant decision, promised deliverable, and material customer-facing instructions verbatim when appropriate for the public repo. Add changes, verification, corrections and remaining work after the quote. Link commits, PRs and CI runs. Mark sensitive omissions with a short label and preserve the engineering consequence. Keep an append-only record, and update the current checkpoint in [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md).

## Offline BRR Board product direction

The owner requested an iOS 27 keyboard layout and behavior, including key placement, extra buttons and suggestions, with rounded edges and tuned Frosted Glass defaults. Liquid Glass rendering was deferred. The owner requested a fully offline app with network permission removed, the BRR Board brand, removal of AI and online media, a custom North Indian Hinglish slang dictionary, and repair of the inline voice issue in DictateKeyboard #359. The owner requested an extremely detailed phase plan, one signed APK after each phase, and an installable build within four to five hours. The [roadmap](BRR_BOARD_ROADMAP.md) records the acceptance criteria and initial implementation scope.

**User**

> I was asking that in this agents.md, you know, I believe Codex can search browsers. So Codex, what it can do is make the UI of this app, of the keyboard look exactly like iOS keyboard, behave like iOS keyboard, keeping the frosted glass blur. We are not going into liquid glass right now. But all the other things from iOS 27, how each and every key placement is, how suggestions are and everything. And also add a custom-made dictionary for Hinglish to the latest Indian, North Indian word slangs from Instagrams and everywhere. Okay. And everything. And also, you know, if you can check then there is a GitHub repository named Dictate Keyboard. And I have opened an issue there that the inline voice search does not work. And the developer said that it is not their issue. But the issue of frosty. So fix that issue as well and all the other things. Think of blind spots and everything. And take charge of this work. And make a proper plan. How will you do all this? Then start doing this. And after four, five hours, when I check, I should have an installable APK with all these things fixed. Also, remove all the unnecessary branding of Frost Keys. Rename it BRR Board everywhere. Remove all the unnecessary AI features, stickers, online attachments and everything because we are not going to give it any of those. It will be 100% offline. Internet permission will be off. And remove all the unnecessary redundant things. Make it fast, good working. And also in the default settings that this is published. I want you to change that to match the iOS amount of blur that is in the keyboard. Like it has frosted glass, not liquid UI. We are not going into liquid UI. But you will change the settings, blurs and every possible thing of that frosted glass to match and look as much as the liquid UI of iOS 27 with the rounded edges and everything. Also, you know, also if you check, you would know how I wanted this keyboard to be, how I wanted my, where I wanted my, I wanted extra buttons and everything. If you would search up the changes I tried to make, you will understand what I was trying to do and do it better. So tell me what is your plan. Create an extremely detailed plan. Create phases that you will apply changes, make changes and get it working. And after every phase, make an APK. And let's go

> So?

**Assistant outcome.** Recorded the roadmap, checked the upstream dictation issue and historical layout PRs, and started the signed offline baseline build.

### Backup-based defaults

> All right, so proceed, dude. What are you waiting for? Implement these one by one or however you think is the best. And let me also give you a backup of my current Frost key. So it will give you all the possible settings. I don't know, maybe dictionaries or emoji keyboard or each and everything. I want you to look at that, identify what it is giving and make it the default of this app that we are developing. Like this backup that I'm providing should already be included in every version of this Frost Keys app because this is the default. Okay? So here it is. And I want the next installable APK, the next update that you will give after doing what you're doing to be something substantial. All right

**Assistant action.** Converted the backup's reusable preferences into source defaults and added its three imported dictionary assets. Retired startup restore of the old full backup and kept explicit preferences intact on upgrade. The new APK verifies the bundled asset bytes in CI.

## Branding and inline dictation priority

> Okay, now do this simple thing. In the next update should be regarding the work that we were doing. But the next update should be erase the term Frost keys from everywhere. And replace it with “brrBoard”. Okay, remove any sign of the original creator because I don't want that. If you want to apply, then you can just remove the sign of the original creator's telegram or any other information like that. Replace it with BRR code. Also add the link to my website. That is, you can see a GitHub repository. I have my own Mangalore. You can add that or that. Because I tried this renaming once but still dude, the app I installed comes up as Frost Keys when I go to select keyboard, turn on keyboard. It comes up at Frost Keys. What the fuck is this? And the language that comes under Frost Keys in keyboard selection is UK. But this is clearly English. So please fix all these small small things. And if you find any other things as well. And then we'll go. Let me just also test this app. I have uninstalled the previous Frost Keys app, means my app that is BR Board. So we'll be calling it normal the board app. This is just for us. But the original name will be what I told you. Okay Okay, now I have installed the application, and in the welcome page it shows Welcome to Frost Keys. Then in the Let's get you started, it shows Enable Frost Keys, Switch to Frost Key, Change it to BR-board. But in the change keyboard now I am getting the BR-board, but the language below it is showing English UK. Also in the APK file and the app installation and in the Obtenium, it is showing me BR-board, but in that it is capital B, capital R, capital R, capital R, capital B, and small O-A-R-D. I don't want that. I want you to replace it with what I told you and remove the Discord, Telegram, or any of in About section, add About me and everything. Okay. And now that I have enabled BR-board, yeah dude, it works fine. Yeah, it is actually installed, how my current BR-board is. Totally like how my current BR-board is. This is nice. But as I tap on the mic button, it changes to the different thing. Why don't you implement the inline keyboard first? Do these. These are the first tasks that you should be doing, dude, come on. Do these and give me the next update in my release section, okay?

**Assistant action.** Adopted the exact brrBoard casing, neutral Android picker label, and MangoLoads About link. Removed upstream community promotions and kept upstream attribution in source notices. Implemented an Android on-device dictation overlay with stop and cancel and added microphone access in the About settings. The versioned release keeps the existing app identity.

## 2026-11-15 — verify the release and preserve the handoff

> Okay, so please check how much your last work was done. I want you to make sure that whatever we are doing, which are adding it to our agents.md and context.md file in the application. And the any AI agent that inherits this if I change the agent should read that. And it's like even if I change the agent now, it should pick off from chat where I'm leaving with you. This amount of context I need there in the GitHub repo that we'll later delete when the all thing is done. Can you do that? Please do that. And also tell me in brief where we are exactly in the process and what are the further things that we need to do

**Assistant outcome.** Verified GitHub published the signed v1.0.8 APK from the rename and inline dictation commit. Compared the debug test failure counts before and after the rename and documented the remaining phone tests. Updated AGENTS.md and PROJECT_CONTEXT.md so the next agent reads the latest owner request, verified release, and next action before taking over. Keep these records during active development and revisit removal when the owner says the project is complete.

## 2026-11-15 — v1.0.8 phone feedback and iOS reference plan

The owner reported a failed mic test after granting microphone access. In the recording, brrBoard briefly displays a light listening panel, then prompts for an offline model. Original FrostKeys opens a separate listening screen. The owner requested whole-toolbar downward drag with finger tracking and roughly halfway collapse, and asked to plan before coding, with mic first, keys and icons second, and drag third. Private recording omitted.

> So these are the images of the iOS 27 keyboard that I could find online. You can search more images and find things online, the leaked something articles, search on multiple trusted sources to give me the best. Now look at, I want it to be smart. Something's I'm telling, some things you have to find out, search yourself and fix and add, okay? I want it to look the most like iOS 27 on my phone that is Oppo Reno 10 5G. You can search up the dimensions and everything without any bugs because last time I faced a lot of bugs and I had to abandon this project. You already suggested several things. Now, the thing is that you can see how in the images That how in the images the keyboard is. What is the roundedness of the corners? How do the suggestions appear? What is the layout? How are the keys? If you can also figure out the frosted blur and the transparency of the keyboard, then also apply that. But add my default settings as the, with the name OG default, so I can always switch back to in one click. I specifically want you to notice that how in my keyboard, you see the globe icon? I single click on it and it changes my input method. The next keyboard. It is switched to the next keyboard. That is my dictate keyboard. In the iOS keyboard photos that I have given you, there is no button like that. Instead, that button is down on the left bottom as an emoji icon. Well, I want that emoji icon to be the iOS-like globe. I click on it, it changes language. I tap and hold on it and it opens a menu exactly in the iOS liquid glass style, a short menu where I can drag and, you know, drop. Where I can drag. I have to keep holding on it, and I can drag to the options that appear there. And if I hold on it, one will be emojis and one will be the clipboard and one will be settings. So the, and it should be in, you know, like half of a semicircle manner around that. In circular manner, if you are getting what I'm getting, and whatever we are doing, whatever changes we are making, please add that to Agent Assembly and contexts. All right? And the mic button on the left right bottom should be what the mic button on our keyboard right now on the top right does. All right? It is really simple. And the toolbar icon on the top left of our keyboard should be moved to, I guess, the top right. As you can see in many images, there is an A written on the right-hand side of the suggestions, like that. And you know we have BRR board written on the keyboard, keyboard spacebar. I want you to make it smaller, and as in the screenshots of the iOS that I have given you, on the spacebar you can see in the right bottom there is a small E-N-E-S written. I want you to write it there in the same manner. I have given you multiple screenshots to reference from. If multiple screenshots that I have given give you a relatively different theme, like the transparency, button sizes, and all, I want you to add them as presets. And I want you to search online and find the best of the best. Also, I want you to take a look at the context-aware key addition, like how if it is an email field, @ appears, how full stop appears, and things like that appears. This context-aware thing is also very important. So I want you to know how will you implement this. And when you copy something, how this copy, how the suggestion appear in the toolbar to paste it when you go to a text field, and how smart context-aware button changes near the spacebar, but originally the spacebar line should be just three things, as in the keyboard: 1 2 3, spacebar, and then enter. Okay, I want you to think thoroughly, search online, more reference, how will you do this, how will you do this better, and the buttons changing and everything, how in originally iOS there are keys and everything, and start doing on it, okay? Don't go head on, make a plan, see the changes, search online what will be changes and tell me what are the changes and how you will implement. I hope you don't break the things in that manner. Review the code for the best functionability. Review the code that it will not create issues and everything. Look for every blind spots. Tell me the plan and then I'll, and then we'll proceed

**Assistant outcome.** Compared the eight design references with Apple guidance, independent reporting, OPPO specifications and the current code. Recorded design interpretations, acceptance checks, and separate implementation commits in the [plan](IOS27_RENO10_PLAN.md). Research and handoff only. Raw screenshots omitted.

## 2026-11-15 — proceed with implementation and wallpaper tint

> Also look at the curved edges of the keyboard and everything. Okay, and I want you to do one thing. In this iOS tinted, I want you to look at one thing. You know, select Apple's color palette. And it should be material 3 expressive, like in Material 3 Expressive, it will, how, I don't know how it does, but it selects a color based on your current wallpaper. And that color should then be seen. Which color in the Apple color palette is the closest to? And it should tint to that level. This is what I want. Tinted but control tinted. Okay and all the click feel, touch feel, drag feel should be like iOS. And make the keyboard more smarter. Don't include any AI feature or anything like that that makes it slow. Keep it fast enough, lightning fast. That is how you want your keyboard. But make it more smarter and let's proceed doing. Okay, let's go. Make a plan what you'll fix first, what you'll fix second, and then start fixing them

**Assistant action.** Added wallpaper accent matching, restrained tint, edge handling and touch checks to the plan. Started the offline mic repair in InlineDictation.kt with an API 33+ support check, same-language regional fallback, themed readiness-based status, deadlines, and clearer model messages. Source review and XML parsing completed. The local Gradle compile attempt failed fetching Gradle 8.14.4 due to network reachability. GitHub build and phone results are tracked in the project context.

**Owner correction.**

> Hey, no, no, no, you are not supposed to blend it into the frosted glass dude, no. No, no, no, no, that's bad. I want white tinted. I meant only the enter key and the other key that the original iOS makes blue or something like that. Not the whole, whole frosted glass dude. No, no, no, that is very bad. Please fix this approach

**Assistant action.** Superseded the earlier surface-blending plan. White Frosted Glass and neutral everyday keys; wallpaper-matched Apple color only for actions iOS visibly highlights, such as Search. The mic checkpoint has no tint implementation.

## 2026-11-15 — resume the iOS appearance work

> Okay, so please continue what you were doing

**Assistant action.** Confirmed the signed v1.0.10 microphone build succeeded. Added iOS Clear and iOS Tinted color options, neutral rounded keycaps, and a small lower-right spacebar caption. Scoped the wallpaper accent to the Search action in iOS Tinted. Continued to use the existing Frosted Glass window and touch geometry. The original Frosted Glass preset remains available. Footer and row rearrangement, full OG default, and gesture changes are still queued. Checked the XML and ran git diff --check before the signed build.

## 2026-11-15 — bundle the layout and toolbar gesture

> Hey, implement the layout next. This update that you gave was really small and a waste of my time. Delete all the unnecessary themes, just keep the OG Frosted Glass and the two themes that you added, and implement the whole layout, each and every button and changes that I wanted, okay? And the small BRR board caption to the lower right is only on iOS, clear and tinted. Add that to every theme and make it less opaque, and implement the whole layout that I suggested. Look at the pictures of the photos that I gave and that, okay? Just do that. The next update should be the layout update, and the whole toolbar swipe to close as well.

**Assistant action.** Combined the new row and footer, right-side toolbar control, interactive downward dismissal, the three theme choices, lower-opacity caption and contextual paste/key changes. Kept native numeric and script-specific layouts. Recorded signed and physical-device verification separately.

## 2026-11-15 — continue from the last layout handoff and preserve video feedback

> Look at my the project, you know, we are doing. Look at other charts of this project. Look at the my BRR board and take up the work from where it ended and complete it

The owner attached a portrait screen recording named `VID_20261115_104614.mp4` (~99 seconds). The recording itself, private data, and entered text are omitted from this public repository. The visual review found an open Settings search field, the 123/space/Search bottom row, the globe/mic footer, a right-side toolbar control, a three-item suggestion strip, the symbols layout and an Android data-usage screen reporting 0 B at the recording time. The exact installed build/theme, hold-menu behavior, whole-strip dismissal, clipboard chip and offline mic were not demonstrated conclusively. Spoken narration, if any, was not reliably transcribed, so no spoken requirement is invented. See the [video checkpoint](IOS27_RENO10_PLAN.md#2026-11-15-owner-screen-recording-checkpoint).

> Also, whatever instructions and whatever you learn from this video, whatever I want, please add that to the Github context and agent page wherever it is supposed to be added okay because that is the file that gives an endless context to any AI agent and I can switch any time, I want

**Assistant action.** Verified the v1.0.13 signing/release workflow succeeded; the independent debug CI built and signed its APK but recorded 14 of 180 failing tests. Inspected the latest implementation and added accessible Emoji, Clipboard and Settings actions to the globe for touch exploration without altering the normal three-shortcut radial hold gesture. Updated `AGENTS.md`, `PROJECT_CONTEXT.md`, this conversation record, the iOS 27 plan and the known-issues checklist to distinguish the owner's written decisions, visual evidence, code state and physical-device tests still pending. This follow-up source patch requires its own signed CI; no claim of phone interaction success is made.

## 2026-11-15 — owner confirms globe shortcuts and dictates defects

> Okay, let me tell you, when I hold the globe, it works fine on the emoji section, bringing the emojis. I have also shown that, I believe. If not, then let me tell you, when I do that, let me do it right now on my phone and tell you, and dictate you what happens. So when I tap and hold on the globe icon, when I tap and hold on the globe icon, the three things appear as intended. And when I leave on emojis, the emojis section appear. When I leave on settings, the settings open. It's totally normal. When I tap and hold and leave it on clipboard, the clipboard opens. But the problem is when emoji and clipboard open, their height is not matched to what is the height of keyboard. Okay. This is their behavior. And all the other behavior I have dictated in the video only. So please fix them

The owner then supplied six iOS-style keyboard comparison images and elaborated in the same turn. The text below preserves the relevant written wording; images remain private in the conversation.

> Okay, let me tell you, when I hold the globe, it works fine on the emoji section, bringing the emojis. I have also shown that, I believe. If not, then let me tell you, when I do that, let me do it right now on my phone and tell you, and dictate you what happens. So when I tap and hold on the globe icon, when I tap and hold on the globe icon, the three things appear as intended. And when I leave on emojis, the emojis section appear. When I leave on settings, the settings open. It's totally normal. When I tap and hold and leave it on clipboard, the clipboard opens. But the problem is when emoji and clipboard open, their height is not matched to what is the height of keyboard. Okay. This is their behavior. And all the other behavior I have dictated in the video only. So please fix them And if you are missing the video transcript, then let me tell you what I saw in the video, in my own words. So the globe and the mic button, the mic button has the older issue that was of the inline dictation through my dictate keyboard dictate voice input does not work. The globe and the my keyboard should be a little bigger and should look like how in iOS 27 looks. Why don't you search online and get those? And there is unnecessary gap between the globe and mic and the bottom row. So there is, so between them there is unnecessary gap that gives the keyboard this extra natural and wanted height. Also the empty toolbar, you know there is this empty toolbar space. Like when the tool, this the top, above the top row, there is this empty space even when nothing is happening. And when things are happening, I want it to be a little slimmer. And this A that is written, this is, this A is written how the mic button was there, but I want it to be as it was in the screenshots. Okay, I'll upload the screenshots with this chat. Or after this chat. These are the things. Also when I, you could look at the behavior when I'm closing the keyboard by swiping down on the toolbar, how buggy it is. I want you to look closely at that and fix that.

**Assistant implementation, not yet owner-confirmed.** Inspected normal/panel measurement, screenshot geometry, the swipe frames and the on-device speech selection logic. Made the normal and panel keyboard height calculations share footer and strip allowance, reduced the strip/footer height, enlarged the footer glyphs, removed the A-button's filled circle, adjusted swipe axis capture/snap/fling, exposed accessible globe actions and allowed a safe English offline model fallback for a Latin-script Hinglish keyboard. Published files do not include the private recording/screenshots or user-typed text. Signed build and Reno10 validation remain pending.

## 2026-11-15 — follow-up handoff from the same project conversation

> The prediction strip sometimes displayed suggestions that appeared unrelated to the entered phrase. Please assess the relevance and usefulness of suggestions, not merely whether they appear.
>
> These interactions already work. Do not redesign or break them.
>
> Switching between the normal keyboard, Emoji and Clipboard should preserve consistent outer height and bottom alignment.
>
> Wait for or incorporate my feedback after installing v1.0.14.
>
> The second video’s spoken narration was not reliably transcribed by the other ChatGPT. If I described additional requirements only in its audio, those still need to be captured accurately. Do not invent them.

**Owner handoff.** The recordings and six references inform compact spacing, larger footer glyphs, a subtle right-side A, appropriate contextual actions, and consistent theme geometry. Hold/release to Emoji, Clipboard and Settings was confirmed by the owner on an earlier phone test. Acceptance requires equal outer panel bounds; a whole-strip, finger-following downward drag, a deliberate fast flick and a smooth short-drag reset; and working offline inline dictation with Stop, Cancel and text insertion. 0 B on the visible Android data-usage screen is a snapshot. White neutral iOS Tinted glass, action-only wallpaper accents, three themes, all-theme lower-right caption, offline operation and update identity remain requirements.

**Verification.** GitHub published signed v1.0.14 from `7ff6e31284fd785767a1a63cec4793d4c8d80f5b`. The exact list of 14 failing debug tests matches v1.0.13. Verified the source changes to height allowances, 36dp strip, 38dp footer, 28dp glyphs, subtler A, swipe and snap behavior, accessible globe actions and installed English model fallback for Latin-script Hinglish. Source review identified no new suggestion-ranking or fully empty-strip collapse change in that release. Accessed the second recording privately and confirmed the audio track; no reliable narration transcript is available. No app code or APK changes in this handoff update. Awaiting v1.0.14 phone feedback.

## 2026-11-15 — owner refines dismissal, footer, and clipboard

> When you pull down on the keyboard, it reacts perfectly to how I told you, pull the toolbar down to close. But in the background, the blur still stays, and when I have pulled the keyboard down, it still closes after that.

> And the A-style symbol, it pushes the whole suggestion strip to a little left. We want it centered, and A should be a little smaller, and it should not be A.

> So this globe button, it should be switched for emoji button. And you tap and hold on it. There you have your settings, your clipboard.

**Owner direction.** Follow the supplied iOS references; center suggestions, use a compact toolbar glyph, enlarge/improve footer symbols, suggest copied text, images and files when compatible, and keep recognition offline. Mic tap is inline, mic hold changes input method. Preserve keyboard height and footer on the enlarged keypad. Enter it by holding alphabetic 123 and remove the duplicate symbols-page keypad entry. Defer momentary hold-and-slide number selection. Review and consider making the proposed external gesture-typing library the default after the owner supplies it.

**Implementation checkpoint.** Window blur is suspended when dragging begins and restored on a canceled drag. The keyboard follows the finger using the existing gesture. The footer now has Emoji and mic tap/hold actions. The new icon has a balanced opposite slot to center the strip. The clipboard offers fresh compatible rich items without transmitting content. Existing next-word ranking was not changed. XML and whitespace validation passed. Signed CI and physical validation are pending at commit time.

## 2026-11-15 — iOS typing audit and swipe import

> Here is the you know the swipe typing library that will be included from here on. But I want you to check what are the behavior of my keyboard at every possible point. And then compare it to the behavior of an iOS 27 keyboard at every possible point. And what are the differences and changes? Implement that and things that are missing, implement them as well. I'm not talking about the liquid glass but all the other things except that. Okay. And if there is something else you want to tell me like, you tell me that as well. What are we missing? What should we do?

**Implementation.** Reviewed Apple iPhone documentation and current source, recorded the behavior matrix, matched the uploaded ARM64 ELF to the existing checksum, and implemented bounded direct ZIP import without executing Magisk scripts. The binary stays user supplied because no redistribution grant was included. Added Space hold trackpad and enabled double-space period by default in appropriate text. Kept the owner-selected themes, footer and swipe dismissal behavior. Physical testing, context-aware prediction quality and remaining differences are documented separately.

## 2026-11-15 — audit behavior conflicts and remove finger-follow dismissal

> Hey, I wanted to do one thing. What are the features that you are doing? Like all the changes that you're making, all the features that are already there. I mean not even, I wanted to look at all of them. For example, you can look at the code and see the behavior that pressing one, two, three button, what does the code say? The code at different places might say different things and that might be conflicting behavior. So you will look at that and you'll find out how iOS solves it and iOS prioritizes what and you'll prioritize that, okay? And you know the swipe down where the keyboard follows my thumb, I think we should remove that because I think it's the phone animation to give its own keyboard swiping down animation because even with the blur removed, the background, you know, the bug flash of keyboard swiping down after I have swiped down the keyboard with my own thumb still persists. So we will keep the slide down but we will remove the custom follow my thumb. But it's the keyboard closes with the phone's original animation of swipe down. Are you getting me? And fix all the bugs, make the code smoother, remove all the conflicting things, all the residual code and everything. And give me the next APK. And review the video again frame by frame to look for more places of improvement And just don't start doing things because I have, I have said so. Think, how would things be implemented? Improve and implement. Every button, you got to think how it behaves in RSS, how when you click on the emoji icon, how many emojis are in a line, how many emojis are shown at one time, and everything like that. All the other things. Like how big should be the space bar key. All right. And to my OG Frost keys, I want you to add an OG Frost tinted where Apple's color palette closest to my Material 3 alternative of wallpaper is implemented on the buttons like backspace, enter, one two three, and capital shift, whatever it is. Buttons like these, okay? So look at how iOS implements, you know, how many and how buttons and symbols are implemented, how each layout of more symbols is implemented and things like that. Because the tap and hold on one two three still does not open the big keyboard that I want it to. So please

## 2026-11-15 — owner asks to confirm continuity and keep batches checkpointed

> Hey, you have done a lot of work but I am like being forced to stop the thinking right now because this thing seems to be stuck at inspecting layout change timing. So I don't know if I, if I give a prompt here, I don't want, you know, fuck it up the process. I don't want to do the whole process again. So tell me if the previous work is preserved. That is, I don't know, a lot of work, like interaction conflict downward gesture currently translates the whole keyboard and disables window blur and then asks Android to hide IME afterwards. All the things that it have been working on. Has it worked or not? Because for right now, it is being, I believe, I don't know, maybe it is stuck at inspecting layout change timing because it should not have been taking too much time here. So I'm a little skeptical. So I am giving you this prompt. Please continue the work and please tell me if it's working or not. Give me an APK with all the changes that you will make

> Hey, please do all this work in small batches because I am afraid all. I waited for an hour or two almost and all the work seems like it is lost because the previous, in the same chat that I am talking to you, previously it was doing something like, let me read it out for you. It was doing something like inspecting layout change timing and checking main branch state. It did something like checking input view difference, evaluating gesture interception. And I believe all of that is lost. Or is it not? Tell me. Please do small changes and please, whatever changes you do, as soon as you do, give that to agent.md or save that somewhere so that we know exactly what change we made, what issue we, what barrier we stumbled into and how we fixed it, what was the issue, so that we know that not to repeat the same issue. And please tell me, have we lost all this work? Because it said that reviewed keyboard logic pointer tracking, project guidance and toolbar state. Before that, inspected toolbar layout, video behavior and pointer tracking. And it updated iOS behavior documents, checked the emoji resizing, resized the emoji as I said in the previous prompt and everything

**Continuity check.** Work was preserved in a separate local worktree and branch, `codex/ios27-behavior-cleanup`, based on current `origin/main` commit `4d61deed437288c9bd4d8574a1fb7647bff116ca` (signed v1.0.16). Before this batch the existing uncommitted source contained edits for the 123 hold, toolbar drag interception/native hide request, panel/footer insets, emoji-scale migration, dictation UI, and theme accents. The owner was told accurately that this source was not yet committed, built, signed or published.

**Batch 1 — checkpoint and source audit.** Inspected `PointerTracker` down/long-press/up paths, `KeyboardState` alphabet/Symbol transitions, `InputView` touch interception, `BoardFooterView` hold arc, dictation state, theme colors, keyboard frame/inset placement, the main keyboard XML and the release workflow. Re-reviewed the 116.8-second recording at one-second intervals and transition frames, plus the 25.7-second clip. Documented that this is not literal manual review of every 30fps frame. Found that the competing `PopupMenu` conflicted with the owner's confirmed arc; current patch has one release-select arc with Emoji, Clipboard and Settings. Updated `AGENTS.md`, `PROJECT_CONTEXT.md`, this conversation log, `IOS27_BEHAVIOR_AUDIT.md` and `IOS27_RENO10_PLAN.md` with source behavior, priorities, and explicit unverified phone checks. No APK has been built in this batch yet.

**Current status.** Source changes remain uncommitted on the isolated branch. The next batch is a whitespace/XML/manifest diff review followed by an Obtainium assemble. The final batch is CI signing/release; stop and report the exact failure instead of claiming an APK if CI does not succeed. The app's prior debug workflow has 14/180 failing tests; no test suite has been run in this task.

**Batch 2 — static validation and local build attempt.** `git fetch origin main` confirmed the base remained `4d61deed437288c9bd4d8574a1fb7647bff116ca`. `git diff --check` passed. The touched keyboard layouts and every localized `strings.xml` parsed as XML. Removed the now-unused long-press-symbols preference from its Java/Kotlin declaration, SettingsValues, advanced settings, default string, and 31 stale translations; repository search found no remaining references. The local command `./gradlew --no-daemon --stacktrace :app:assembleObtainium -PobtainiumBuild=17` failed before Gradle configuration because the wrapper needed Gradle 8.14.4 and the workspace reports `Network is unreachable`. This does not prove or disprove source compilation. Next batch is the remote signed Actions build; no tests were run.

**Batch 3 — saved commit and authentication blocker.** Committed the code and documentation as local commit `a73dfa490afb92037cd82e337a9403f18b39369e` (`Implement iOS-inspired keyboard behavior cleanup`). `git push origin HEAD:main` failed before contacting GitHub because the shell has no username/password or device login (`fatal: could not read Username for 'https://github.com': No such device or address`). The local commit is intact and the worktree is clean. No GitHub workflow or APK release has started. Next use the linked GitHub integration, if authorized, to publish the commit; otherwise the owner must push it. Do not describe source changes as a delivered APK.

## 2026-11-15 — checkpoint granularity and language scope

> Hey, I want you to say one thing to you. If you keep documenting every one step, you'll waste up 50% of my time and your two tokens, my two tokens in just documenting things. So create small batches, okay? And keep me updated on that. Like for example, if you're working on thing, you might have a plan. So let's say it is a 10-step plan. So divide the work into 10, 20 or whatever batch is perfect for you in that amount. And say one batch done, two batch done, two of three, three of three done, four of ten done, and like that. So I also stay updated while you're telling me that, okay, now we are at the fifth stage or sixth stage out of ten. And I see you are creating Chinese keyboard setting resources, Turkish keyboard setting, translation resources. What is this shit? I don't want any of these. Just English. And English UK and US. Just keep them as backup. I don't know. Like don't delete them, but I'll only be using English, H-I-N-G-I-L-I-S-H. Nothing else. So don't waste your time on this Tamil, Russian, Romanian, Spanish shit. I guess this will help you fast track the work and remove all of this if it is not necessary. This will make the app faster, more optimized and save your time and app space

**Owner clarification.** Work in a few meaningful batches with concise progress counts and checkpoint at batch boundaries; avoid logging every tiny step. Active language use should be English and Hinglish, with English US/UK retained. Keep existing locale resources as backups; do not add or spend this milestone polishing unrelated language translations. Verify exposed subtypes/layouts before any removal. No locale files were created or deleted in this batch.

## 2026-11-15 — remote build failure and import-only recovery

**Batch 1 of 3: preserved work and CI diagnosis.** Personal project history, Git status, and GitHub Actions confirm the behavior audit, video review, source changes and handoff updates were preserved. The source/context tree is on GitHub `main` at `10ca174`; local commits `a73dfa4` and `e1715e2` retain the same code checkpoint. Obtainium run `43719397173` completed and failed in `:app:compileObtainiumKotlin`; exact diagnostics were unresolved `OgFrostAccentColors` at `KeyboardTheme.kt:376` and unresolved `View` at `BoardFooterView.kt:171–172`. Signing and release were skipped; the artifact list is empty. Debug run `43719397116` failed at build as well. Therefore the source work is preserved, but this batch has not produced an APK.

**Batch 2 of 3: compile fix in progress.** Add the missing import for `helium314.keyboard.latin.common.OgFrostAccentColors` and `android.view.View`; this is a compile-only correction and does not alter interaction behavior. Update the context checkpoint in the same commit. Next run the signed workflow; only if it passes will the APK be handed over. No unit tests have been run for this work (prior debug baseline remains 14/180 failures). Device behavior remains unverified.

**Batch 3 of 3 — signed release completed.** Compile-only imports were added in local commit `1010e19`, mirrored to GitHub `main` as `65078e09c0d3bcebc3702c5bace5a56bd6814510`. Obtainium run [43720145629](https://github.com/Batmancpu/brrrBoard/actions/runs/43720145629) passed the unsigned build, signer verification and release steps, publishing [v1.0.18](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.18) with [`brrBoard.apk`](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.18/brrBoard.apk). This confirms compile/package/signing, not Reno10 interactions. The independent debug run [43720145633](https://github.com/Batmancpu/brrrBoard/actions/runs/43720145633) successfully built, verified its debug signer and uploaded an artifact, then failed its automatic unit-test gate: 13 of 180 tests failed (5 ParserTest, 7 InputLogicTest, 1 StringUtilsTest). No manual test command was run. Do not claim tests are green or attribute these failures to the patch without baseline comparison.

**Language/resource clarification.** The many keyboard subtype and localized string resources are inherited from the existing upstream tree; this work did not create or delete them. Keep them as backups. Focus the owner's use on English/Hinglish with English US/UK retained; avoid unrelated translation work.

**Next checkpoint.** Await the owner’s v1.0.18 Reno10 report. Ask for the result of update-over-install, swipe-dismiss blur/flicker, 123 long-hold numpad, normal/Emoji/Clipboard height alignment and offline dictation. These remain device-only checks.

## 2026-11-15 — prevent host content from sitting under the keyboard

> Hey also one additional thing that I did not notice while making the video that throughout the video you would see the keyboard is eating up the content that is supposed to start from where the keyboard ends. Or just after where the keyboard ends. So please fix that as well in all keyboard heights. And all the other changes that we have discussed so far before. For example, the height, default height that you just verified and everything. Make that happen as well. And I have also spoken a lot in video in Hindi. If that is fine by you, please check the video frame by frame, listen to my audio, what I have said, listen to the changes and implement all those. And all the, and some things, probably there's going to be many things that I missed and couldn't say. You just see, notice, change them as you want th…1619 tokens truncated…no and continue

**Request captured.** Implement the detailed requirements consolidated in [the post-v1.0.19 Reno10 plan](POST_V1_0_19_RENO10_PLAN.md): reliable iOS-inspired behavior on OPPO Reno10 5G; stable content/keyboard boundaries and panel sizes; balanced Emoji/mic footer; system-following OG dark/light with optional action-key tint; inline use of the configured recognition service; compact offline brrPersonal with separate spelling-valid/text-suggestion/emoji behavior; and evidence-based dictionary/gesture tuning. Keep only changes that improve reliability or match the agreed reference. The user explicitly requested that, after this project goal is reached, I suggest next steps and then wait for their reply.

**Starting point checked.** Current GitHub main is e989e77; application code still corresponds to signed v1.0.19 source 4a6bbfa. Latest post-release commits so far are documentation only. The current branch listing does not contain the transcript's previously described codex/hinglish-voice-ios-icons draft, and the exact 252-word draft list is not present in the repository. The workspace has no local Git checkout, so no uncommitted local source can be inspected or preserved here; current source is being reviewed through GitHub. Raw screenshots, video frames, and private typed content are not copied into the public repository.

**First source finding.** InlineDictation.kt uses SpeechRecognizer.createOnDeviceSpeechRecognizer and checks only on-device availability, while the owner wants the configured system recognition service inline. EXTRA_PREFER_OFFLINE is already requested, but Android exposes it as a provider preference, not a guarantee. RoundedKeyboardFrameView adds the system bottom inset as frame padding, and LatinIME.onComputeInsets subtracts it when reporting the host-content boundary; the visual bottom gap therefore needs a measured layout fix, not removal of the content inset. BoardFooterView already preserves the Emoji hold arc and mic hold-to-switch.

**Checkpoint status.** Batch 0 (personalized README, plan and agent guidance) is on GitHub. PROJECT_CONTEXT.md now records the current source truth, missing draft, and exact next action. No application code has changed yet in this continuation, and no new APK is claimed. The app-code push path in .github/workflows/obtainium-release.yml automatically starts the signed Obtainium build/release; group each coherent app-code batch into one GitHub commit, then verify that workflow before calling the build complete. Physical Reno10 acceptance remains separate.

**Next action.** Finish tracing the relevant theme, suggestion and dictionary paths, then make the smallest supported source batch for default-service inline dictation and run the signed GitHub build. Continue with layout, theme/icon and dictionary batches only where current source and the agreed requirements support a concrete change.

## 2026-11-15 — DictateKeyboard issue and selected recognition service clarified

> So let me tell you while you are here, I use this dictate keyboard. And I have my own API in it which connects to internet and give me the best transcriptions. That will be the only voice transcriber that I'll be using because I have purchased it on Play Store. And it has the ability to register as the Android's official voice to text engine. So, and I have enabled that. So, and this repository is also available on GitHub. You can check how it works, how it connects. So you can configure it best. Okay, keep on working please. If there are any other questions that you want to ask, ask me right now. Then I'll go to sleep

Repository supplied: https://github.com/DevEmperor/DictateKeyboard

**Issue read.** The owner's report is [DictateKeyboard issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359). The maintainer explains three Android routes: the standard RecognitionService/SpeechRecognizer route leaves the calling keyboard visible; Dictate's voice-input IME replaces it; ACTION_RECOGNIZE_SPEECH opens a popup. Dictate registers all three independently. The maintainer says the invisible route works when the caller uses Android's system-default recognizer and Dictate is selected. This exactly explains the brrBoard mismatch: the current source forces createOnDeviceSpeechRecognizer(), bypassing the selected recognition service.

**Code decision.** Use SpeechRecognizer.createSpeechRecognizer() and the existing RecognitionListener/InputConnection result path. Remove the on-device-availability gate, checkRecognitionSupport call, and EXTRA_PREFER_OFFLINE request. The owner explicitly uses Dictate's configured internet API as the sole transcription provider. Keep mic-hold input switching and the private/password/no-personalized-learning guards. Keep brrBoard's INTERNET permission removed; the selected Dictate app is responsible for its provider/network connection. Do not use Dictate's separate voice-input IME, popup Activity, hard-coded service package, or provider credentials.

**Evidence and verification.** Dictate's current AndroidManifest registers DictateRecognitionService with android.permission.BIND_RECOGNITION_SERVICE and separately registers DictateVoiceInputMethodService and RecognitionActivity. Its RecognitionService starts RecognitionSession using the provider configured in Dictate; it does not read the caller's on-device/offline preference. Android's official SpeechRecognizer API says createSpeechRecognizer(Context) uses the system default service; it also documents the RecognitionService query requirement. brrBoard's manifest already declares that query and RECORD_AUDIO permission. The Dictate maintainer's guidance is at https://github.com/DevEmperor/DictateKeyboard/issues/359; Android API docs: https://developer.android.com/reference/android/speech/SpeechRecognizer.

**Privacy documentation.** Updated the public README/privacy policy to distinguish local keyboard suggestions from the selected dictation provider: the configured Dictate provider may send audio over its network service. brrBoard itself receives no INTERNET permission and does not store the provider's API key.

**Prepared source patch.** InlineDictation.kt now uses SpeechRecognizer.createSpeechRecognizer() and SpeechRecognizer.isRecognitionAvailable(), removes the on-device model probe and EXTRA_PREFER_OFFLINE hint, retains final-result-only insertion, and allows a 30-second provider response window after stop/end-of-speech. This is prepared but not yet committed; no compilation result is claimed.

**Next action.** Commit this coherent source/docs batch atomically to main so the signed Obtainium workflow runs once. Confirm CI build, signature, package/version and no-network-permission checks; then continue only the confirmed Reno10 layout/theme/dictionary work. Dictation runtime still needs device confirmation with network enabled.


## 2026-11-15 — Reno10 Dictate language result and final retrospective request

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work.

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show me English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Owner device report.** The owner reports that v1.0.20's mic route reaches Dictate on Reno10, confirming the selected provider path is active. The resulting text is Hindi, so English-only dictation is not accepted yet. This is owner-reported; no device screenshot/log was provided or copied into the public repo.

**Provider source review.** Read Dictate's current `DictateRecognitionService.kt`, `RecognitionSession.kt`, `DictateLanguages.kt`, `DictateLanguagesScreen.kt`, and `DictateController.kt`. `onStartListening(recognizerIntent, ...)` does not inspect `recognizerIntent`; RecognitionSession uses DictateController, whose transcription reads Dictate's saved activeInputLanguage. `languageFollowsKeyboard` can map a hi-Latn keyboard to Hindi inside Dictate. This means an English `RecognizerIntent.EXTRA_LANGUAGE` is the correct standards-compliant request for providers that honor it, but it cannot override Dictate's saved language. The owner needs Dictate Active language = English and language-following off for English-only output through this current Dictate build. brrBoard cannot safely or legitimately modify another app's private preference.

**Source change in this batch.** InlineDictation now requests the current English keyboard region when the locale is English, and requests en-IN for any other keyboard locale. It no longer maps Romanized Hindi keyboard input to hi-IN. Signed release v1.0.21 built and passed signer verification in Obtainium run 43751780751; APK SHA-256 is 4eadc40ca077bda53f0c2217bcaeb319185c252629e8a802c5b7a0af3f45f961. The owner has not yet retested English output with Dictate set to English, so that behavior remains unverified.

**Project-level final acceptance instruction.** When the planned implementation reaches its end, reread all post-v1.0.19 user requests in this record and the screenshot corrections, verify each against source and build evidence, and implement any supported omission or defect before proposing follow-up work. The user explicitly requests this retrospective; it is a required completion step, not optional cleanup.

## 2026-11-15 — Three-slot center autocorrection treatment

**Reference behavior.** The supplied screenshot shows a three-slot suggestion strip, a highlighted center candidate when the keyboard is confident, and a separate compact control at the right. The app already has a three-slot strip and a decoder-owned `mWillAutoCorrect` decision; there is no need to invent or tune a new score.

**Source change in current batch.** In `SuggestionStripLayoutHelper.layoutWord`, add a rounded neutral inset background only to the center item when its tag is `INDEX_OF_AUTO_CORRECTION` and `SuggestedWords.mWillAutoCorrect` is true. Side candidates retain the normal strip background, and the five-chip optional mode is unchanged. The background uses the existing `AUTOFILL_BACKGROUND_CHIP` theme color for light/dark contrast. CI build is pending.

**Proofreading clarification.** Apple's iOS 27 Writing Tools access point can launch Apple Intelligence Proofread on selected text; Apple documents it as a system writing feature for grammar and spelling, not a predictive-word engine. brrBoard's Android spellchecker is word-level and cannot invoke Apple's private Writing Tools. Preserve the compact board toolbar control; do not label it as grammar proofread or add a cloud/AI proofreader. Sources: Apple Support https://support.apple.com/guide/iphone/writing-tools-write-improve-summarize-iph6f08da1d2/27/ios/27 ; Android SpellCheckerService https://developer.android.com/reference/android/service/textservice/package-summary.


## 2026-11-15 — Continue from v1.0.22 and Hindi Dictate report

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Checkpoint.** Obtainium run 43752544242 completed successfully and published v1.0.22, source commit `f4cfad6cf4ecbfda3a36c63495855dafb818d8bc`. APK SHA-256: `c399a48acbb8fd864130a069f4d0fdffa6c72df47d3ea225811940b1ed3970e4`. The shipped center-suggestion treatment uses the decoder's existing `mWillAutoCorrect` decision and does not alter candidate ranking. The user has not reported Reno10 acceptance of that visual change.

**Dictate language status.** English-only transcription remains open. Current Dictate RecognitionService ignores caller language extras and uses Dictate's saved Active language; brrBoard cannot change Dictate's private setting. Do not claim the English outcome fixed until the owner sets Dictate Active language to English, disables language-following, and reports a successful test. Avoid asking the owner to retest during this continuation; user explicitly asked work to continue.

**Next action.** Continue the code review in the agreed batches, checkpoint each source batch and signed APK in this repository, then conduct the user's requested full retrospective before suggesting next steps.


## 2026-11-15 — Consolidated OG themes and action-key tint (Batch 4a)

**Owner decision from the pasted continuation**

> I don't want the themes shit. I want just three themes. Okay, and this behavior should be automatic. One is the normal dark mode theme, normal light mode theme. The normal dark mode theme is what I have right now, and the normal light mode theme is also the OG theme that is already there, that I have configured myself. I don't know about the light theme; I use the dark theme only. And there should be an option to enable tint. And the behavior should be same. It should match the Material 3 expressive — take the Material 3 expressive color from the wallpaper or ask the system and match it to the closest Apple palette color and just paste it there.

**Source findings.** The prior frosted renderer blended Android wallpaper accent colors into the glass background and normal letter keycaps whether tint was selected or not. Existing nearestAppleAccent already maps system_accent1_500 to the nearest Apple-inspired palette entry with Lab color distance, and OgFrostAccentColors already isolates an action-key accent. Automatic mode was gated by a preference, and four board presets were exposed.

**Source change in Batch 4a.** KeyboardTheme.kt now follows system light/dark, exposes only the OG frosted base choice, and reads a single optional action-tint preference. The glass and ordinary keycaps blend neutral system colors; the wallpaper accent is mapped through the existing nearest-Apple palette helper and applied to action, functional, special, and editor-action key fills only when enabled. A one-time preference migration carries existing OG/iOS tinted selections into the toggle without rewriting saved color values. AppearanceScreen.kt, Settings.java, Defaults.kt, and the default strings resource add the switch and remove separate theme-color/day-night selectors from the visible list. The signed build is pending; no Reno10 visual result is claimed.

**Next action.** Verify Obtainium build and signer, then continue the remaining layout/inset, icon, dictionary and swipe compatibility work. Preserve the device-only acceptance list and complete the required full retrospective.


## 2026-11-15 — v1.0.23 theme release, Shift glyph batch and Dictate language constraint

**Latest owner instruction**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**v1.0.23 release verification.** Obtainium run [43754590416](https://github.com/Batmancpu/brrrBoard/actions/runs/43754590416) passed and published [v1.0.23](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.23) from source `e2ea3b475605b7fed8f57593cddbd8c8bb15b69b`. APK SHA-256: `b772aded5cb5847f2fd78a19c86febc67ebce832cbff14a879471c107686f577`. This ships automatic system dark/light, the single optional Apple-palette action-key tint, neutral OG glass and ordinary keycaps. Device appearance remains unverified.

**Dictate issue and source recheck.** Re-read the owner's [issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359) and the current DictateRecognitionService. The maintainer confirms that Android's system RecognitionService lets the calling keyboard remain visible. Dictate's current `onStartListening` ignores the supplied RecognizerIntent and starts its session using Dictate's own saved active language. brrBoard already requests English (`en-IN` for non-English keyboard locale) and cannot set Dictate's private active-language preference. Therefore the owner's Hindi transcription report remains unresolved; English-only behavior requires Dictate's Active language to be English with language-following disabled, then a Reno10 retest. No claim of an app-side language fix is made.

**Batch 4b source change.** The two supplied keyboard screenshots show the idle Shift key as a thin outline arrow. Source maps the default idle Shift state to a filled Android asset. Added original vector assets for outlined idle Shift, filled one-shot Shift, and underlined Caps Lock; updated Holo/Material/rounded defaults and the stock custom icon mapping to use the same three state silhouettes. App code commit [`cc3ee3b0b6bda626eee5c5f5aca55c4acfd4c65a`](https://github.com/Batmancpu/brrrBoard/commit/cc3ee3b0b6bda626eee5c5f5aca55c4acfd4c65a) is on `main`. Obtainium run [43756115259](https://github.com/Batmancpu/brrrBoard/actions/runs/43756115259) and debug run [43756115209](https://github.com/Batmancpu/brrrBoard/actions/runs/43756115209) were automatically triggered; their results will be recorded in a follow-up checkpoint. No tests were manually run and no Reno10 visual acceptance is claimed.

**Next action.** Record the signed CI result, keep Dictate English-only unresolved until the owner configures/retests it, then continue source-confirmed visual issues. Finish only after the required request-by-request review of the post-v1.0.19 conversation and both screenshots.



## 2026-11-15 — v1.0.24 Shift glyph build and saved-map migration

**Build result.** Obtainium run [43756115259](https://github.com/Batmancpu/brrrBoard/actions/runs/43756115259) passed and published [v1.0.24](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.24), source `cc3ee3b0b6bda626eee5c5f5aca55c4acfd4c65a`. APK SHA-256: `efff92614db7f8303000521d8d6477dbc14691f46750e5a0b53e1b9209f5ffd9`. The release compiles, passes pinned signer verification, and preserves package/update identity.

**Existing-install edge case.** Reviewing `CustomizeIconsDialog.kt` showed it saves the entire default icon map when one icon is customized. An existing preference can therefore retain the old Shift asset after a default change. Added a one-time migration in [`AppUpgrade.kt`](https://github.com/Batmancpu/brrrBoard/blob/4d13a045d5fd17bcb427e2f22e5a70cef00e9049/app/src/main/java/helium314/keyboard/latin/AppUpgrade.kt): it replaces only exact old shipped asset names for idle Shift, one-shot Shift, and Caps Lock, preserving other user-selected glyphs and custom icons. Source commit [`4d13a045d5fd17bcb427e2f22e5a70cef00e9049`](https://github.com/Batmancpu/brrrBoard/commit/4d13a045d5fd17bcb427e2f22e5a70cef00e9049). Obtainium run [43756893374](https://github.com/Batmancpu/brrrBoard/actions/runs/43756893374) is pending. v1.0.24 is interim for installs with a saved old icon map; wait for the migration release to accept the Shift correction.

**Automatic debug workflow.** Run [43754830187](https://github.com/Batmancpu/brrrBoard/actions/runs/43754830187) compiled and signer-verified a debug APK, then failed the unit-test gate: 13 of 180 tests failed (ParserTest, InputLogicTest, and Emoji detection). No tests were manually run. Obtainium release CI independently passed.

**Dictate constraint.** The owner reports that the selected Dictate service receives speech but returns Hindi, and requires English only. Dictate's current RecognitionService ignores brrBoard's language extra and uses its saved Active language. brrBoard cannot alter that private preference. Set Dictate Active language to English and disable keyboard-following, then verify on Reno10; no success is claimed yet.

**Next action.** Record run #25's signed result, then continue source-backed keyboard icon/layout and dictionary review. Do not call the overall goal complete until the explicit post-v1.0.19 retrospective and device-only acceptance list are addressed.



## 2026-11-15 — v1.0.25 saved-icon migration release

**Build result.** Obtainium run [43756893374](https://github.com/Batmancpu/brrrBoard/actions/runs/43756893374) passed and published [v1.0.25](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.25), source `4d13a045d5fd17bcb427e2f22e5a70cef00e9049`. APK SHA-256: `8f7e895eb402892b6dd6f8b3fba61ad04b7f7efe0979d4e507e386d174555c95`. The build and pinned signer checks passed; package/update identity was preserved.

**Effective Shift update.** v1.0.24 changed the default Shift assets, but the customization dialog can persist the old default map. v1.0.25's migration updates only those exact saved Shift defaults, preserves unrelated user-selected icons, and makes the screenshot-backed Shift correction effective for existing installs. Source changes and build are complete; the owner has not yet visually accepted the result on Reno10.

**Current Dictate constraint.** The owner still requires English-only, no Hindi. brrBoard asks the selected RecognitionService for English; Dictate's current RecognitionService ignores caller language extras and uses Dictate's saved Active language. The app cannot modify that private value. This remains a device setup/acceptance blocker, not a claimed code fix.

**Next action.** Continue the remaining screenshot-visible icon/accessory audit and source-backed geometry work, followed by the finite brrPersonal and emoji/swipe evaluation. Complete the explicit retrospective before suggesting follow-up work.



## 2026-11-15 — English-only dictation safeguard and corrected provider copy

**Source audit finding.** The selected RecognitionService route was already active, but the visible inline-dictation error and permission text still described an offline/on-device recognizer. That copy no longer matched the owner-selected Dictate service, which may use the owner's network provider. Replaced those messages with selected-service wording.

**Source change.** On final results, InlineDictation now checks for Devanagari characters. If present, it does not insert them and shows a local message directing the owner to set Dictate's Active language to English. The check is intentionally a script safeguard, not a language detector: it cannot identify Hindi written in Latin letters and cannot change Dictate's private Active language. The system service is still requested with the English locale, and no brrBoard INTERNET permission or provider credentials are added.

**Verification.** Source and the two user-facing strings are committed in `cc6c1ae5048204740c0186f72e233671a410571a`. Obtainium run [43758269031](https://github.com/Batmancpu/brrrBoard/actions/runs/43758269031) passed and published [v1.0.26](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.26), APK SHA-256 `cb95cac9b41c50ddc57f934f5d3040152a45a4ec7cecb3e1ff6beae3f2d41eba`. The separate debug run [43758268969](https://github.com/Batmancpu/brrrBoard/actions/runs/43758268969) built, verified and uploaded its APK, then failed the automatic test gate with 13/180 unit-test failures. No tests were manually run. Reno10 behavior remains unverified.


## 2026-11-15 UTC / Nov 16 IST — v1.0.26 release and continued whole-project review

**Owner**
> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work.

**Owner**
> Also, I want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Source and provider review.** Read the owner's DictateKeyboard issue [#359](https://github.com/DevEmperor/DictateKeyboard/issues/359) and the maintainer's explanation. The system `SpeechRecognizer` → selected Dictate `RecognitionService` route leaves brrBoard visible and delivers results through callbacks. The voice-input IME route replaces brrBoard; `ACTION_RECOGNIZE_SPEECH` opens another activity. Dictate's implementation uses its saved Active language and ignores the caller's requested language extras.

**v1.0.26 result.** Main commit `cc6c1ae5048204740c0186f72e233671a410571a` adds a Devanagari script guard: final output containing Devanagari is not inserted; a local message directs the owner to Dictate's English Active language. It also removes obsolete “offline/on-device” claims from dictation copy. Obtainium run [43758269031](https://github.com/Batmancpu/brrrBoard/actions/runs/43758269031) passed and published [v1.0.26](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.26). APK SHA-256: `cb95cac9b41c50ddc57f934f5d3040152a45a4ec7cecb3e1ff6beae3f2d41eba`.

**Limit.** The guard catches Devanagari characters in `U+0900..U+097F` and `U+A8E0..U+A8FF`; it is not a language detector and cannot detect Romanized Hindi or change Dictate's private preference. Set Dictate Active language to English and disable keyboard-following. English-only Reno10 acceptance is still pending. brrBoard remains without INTERNET permission; the selected provider may use its own network service.

**CI.** Automatic debug run [43758268969](https://github.com/Batmancpu/brrrBoard/actions/runs/43758268969) built, signer-verified and uploaded the debug APK (artifact [14178932977](https://api.github.com/repos/Batmancpu/brrrBoard/actions/artifacts/14178932977), 59.2 MB, expires Nov 29). Its test report found 13/180 failures: five KeyboardParserTest currency/popup cases, seven InputLogicTest URL/autospace cases, and StringUtilsTest.isEmojiDetectsAllAvailableEmojis. Obtainium build passed independently. This batch did not change tests or run them manually.

**Next.** Inspect DictionaryFacilitatorImpl/Suggest and the existing native proximity decoder to design a finite, license-safe `brrPersonal` source. Keep valid-only profanity/slang separate from completions and autocorrection. Do not add a separate hand-weighted ranker. After that, resume confirmed geometry/accessory/icon items and finish with the owner-requested retrospective.


## 2026-11-15 UTC / Nov 16 IST — v1.0.29 Dictate result handling and whole-project continuation

**Owner**
> Also, I want to tell you the APK that you released. I believe it is finally being routed through Dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left.

**Owner**
> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chat is reached, please take a look back at the whole chat and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work.

**Source audit.** Read DictateKeyboard's current `DictateRecognitionService`, `RecognitionSession`, and language settings screen, and reread the owner's issue [#359](https://github.com/DevEmperor/DictateKeyboard/issues/359). The service ignores brrBoard's recognizer Intent and starts a session from Dictate's saved Active language. The settings screen exposes **Active language** and **Follow keyboard language**; brrBoard has no public API to change those app-private preferences. Dictate's current system-service bridge returns one final transcript, even when a caller requests multiple results.

**v1.0.29 source change.** `InlineDictation.kt` now asks for up to three alternatives and chooses the first result without Devanagari if the service provides alternatives; when all returned results are Hindi-script, it inserts nothing. The Hindi-result toast now gives the exact Dictate navigation: **Languages → Active language = English; Follow keyboard language = off**, and stays visible longer. This is a Hindi-script safeguard and alternative-result fallback; it cannot detect Romanized Hindi or make Dictate transcribe English.

**Verification.** Obtainium run [43760989572](https://github.com/Batmancpu/brrrBoard/actions/runs/43760989572) passed and published [v1.0.29](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.29), APK SHA-256 `c4c02913a3cced3758914b34356687026e6c185523b22eac52f86f5532f1c914`. Debug run [43760989575](https://github.com/Batmancpu/brrrBoard/actions/runs/43760989575) compiled, signer-verified and uploaded its APK, then failed the automatic `:app:testRunTestsUnitTest` gate with 13/180 known failures. No tests were manually run. Reno10 English transcription remains owner-unverified.

**Dictionary research checkpoint.** The existing `main_hi-Latn.dict` is a 204 KB packaged binary; the old local 252-word draft is not recoverable. PHINC offers 13,738 manually annotated Romanized Hindi-English social-media sentence pairs under CC-BY-4.0, but its dataset is from 2020 and is not evidence of current Instagram slang. A third-party pinned tokenizer script confirms the corpus can produce a reproducible frequency list, but no corpus-derived list has been added to brrBoard. Any use must add attribution and retain the existing native proximity decoder; no fixed handwritten ranking formula is approved. References: [PHINC dataset](https://huggingface.co/datasets/LingoIITGN/PHINC) and [Clink's pinned PHINC preparation script](https://github.com/anti-ltd/clink-language-packs/blob/2cc15342f1824622ba3d2b9f9379c89ff3bbdb59/tools/prepare-hinglish.py).

**Next checkpoint.** Continue the dictionary/proximity-decoder audit, then implement only a corpus and validity/suggestion split that can preserve swipe decoding while keeping profanity/slang out of proactive suggestions and autocorrection. Finish the request-by-request retrospective, then list only the remaining Reno10 checks that require the owner's device.

## 2026-11-15 continuation — English-only Dictate and final audit

### Owner request (verbatim)

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

### Verified response and next work

- Repository head before this continuation was 6fe4917875fe861d9761d0b330d84cf6ddd65617; latest signed APK was v1.0.29.
- brrBoard binds to Android's selected SpeechRecognizer/RecognitionService and sends an English hint. The DictateRecognitionService source used by the project ignores that caller intent and reads Dictate's private saved Active language. brrBoard cannot silently change that setting. v1.0.29 rejects Devanagari-only results and asks for Dictate → Languages → Active language = English, Follow keyboard language = off. Latin-script Hindi remains indistinguishable from English using a reliable source signal.
- Began the next code batch: add an offline brrPersonal dictionary by reusing the native spatial decoder; use a pinned, attributed PHINC baseline plus a small manually curated valid-only slang/profanity list and emoji shortcuts. Keep manual no-suggest terms out of ordinary completions/autocorrect, while retaining them as deliberate swipe results. Do not introduce a separate ranking formula.
- The owner expects continued small source/release/context checkpoints and a final audit of every post-v1.0.19 request and both screenshots. The current batch is not the end of the project; device checks remain owner-only.


### Dictionary CI correction and Emoji implementation audit

**Build result.** The first brrPersonal commit, `db36ad5ed77d2c681cfaa9986ad7e0f3a76f3e25`, triggered Obtainium run [43766256522](https://github.com/Batmancpu/brrrBoard/actions/runs/43766256522). The automatic build failed before Java/Kotlin compilation because the pinned PHINC CSV header is `Sentence,English_Translation` and the generator only recognized headers containing “hinglish” or “code mixed.” No signed APK or release was produced.

**Correction.** The generator now recognizes the pinned source column `Sentence`. The release workflow path filter also includes `tools/**` so a future change confined to the generator triggers the signed build.

**Emoji audit finding.** `EmojiPalettesView.AdaptiveEmojiEngine` already records local emoji use counts and recency and can cache a top-10 fast row, while the emoji use/recency cache has no presentation call site in the audited source. `addRecentEmoji()` records use and then calls `addRecentKey()`, which records the same emoji again. This is an existing partial implementation, not a finished Smart Emoji panel; remove the double count and integrate the ranker into a compact Reno10-compatible row in a follow-up batch.


## 2026-11-15 continuation — English-only Dictate and Smart Emoji

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Assistant outcome.** Confirmed the v1.0.31 signed release and debug CI result. The user requires English-only Dictate transcription. Dictate's RecognitionService ignores caller language extras and follows its own Active language; brrBoard blocks Devanagari final results but cannot change Dictate's private preference or reliably identify Romanized Hindi. Keep that behavior open until the owner sets Dictate → Languages → Active language = English, turns off Follow keyboard language, and verifies on Reno10.

The Smart Emoji source batch reuses the existing Recents category and local use/recency ranker, prioritizes up to three rows based on current grid width, refreshes its cache after a use, fixes duplicate counting, and records selected Emoji candidates once. The signed CI result for this source batch is pending; Reno10 appearance/quality checks remain open. The final full-transcript audit is still required.


### Smart Emoji compile correction

**CI result.** Obtainium [43769250859](https://github.com/Batmancpu/brrrBoard/actions/runs/43769250859) and debug [43769250853](https://github.com/Batmancpu/brrrBoard/actions/runs/43769250853) both stopped during Java compilation. `EmojiCategory.java:287` referenced `TextUtils` without an import. No release APK was produced.

**Correction.** Replace that call with `outputText == null || outputText.isEmpty()`. The source correction is committed separately with updated checkpoint notes; v1.0.31 remains the latest signed release pending the corrected CI run.

## 2026-11-15 — English-only Dictate feedback and continuation

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left.

**Assistant outcome.** Confirmed signed v1.0.35, including the learned-history reset, and rechecked Dictate's RecognitionService route and issue #359. Dictate starts its own configured recognition session and does not use caller language extras; brrBoard requests English and rejects Devanagari output, but cannot change Dictate's saved Active language or reliably classify Romanized Hindi. The owner must set Dictate → Languages → Active language = English, disable Follow keyboard language, and verify on Reno10 before English-only is accepted. The complete transcript/source audit remains required.

## 2026-11-15 — learned-history reset build correction

**Assistant outcome.** The first builds for the confirmation-protected reset action failed before APK creation. Obtainium run [43771462834](https://github.com/Batmancpu/brrrBoard/actions/runs/43771462834) reported AAPT's `Invalid unicode escape sequence in string` for the new confirmation resource; debug run [43771462845](https://github.com/Batmancpu/brrrBoard/actions/runs/43771462845) failed the same resource merge. The wording contained unescaped possessive apostrophes. The string is now apostrophe-free, the feature remains unshipped, and corrected signed CI is pending.

## 2026-11-15 — active plan and historical documents

**Assistant outcome.** Clarified that the current post-v1.0.19 Reno10 plan is authoritative. The older v1.0.8 roadmap and v1.0.19 plan/behavior audit now point to current status and are labeled historical, especially where their old on-device dictation route conflicts with the owner's selected Dictate RecognitionService.

## 2026-11-15 — learned-history reset shipped

**Assistant outcome.** The corrected confirmation text passed the signed Obtainium workflow. Release [v1.0.35](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.35) is from commit `acacf329aaf058446d5deb4b463a11f8941cc0ce`; the APK is 51,024,650 bytes with SHA-256 `dc4beadfa10a1d13476b5c978bcdebe96231fed2dba9d55da52a3d1c08a40b15`. The debug APK built and passed signer verification, while its automatic unit-test gate was still running. No tests were manually run. The first resource builds failed and the failure plus apostrophe-free correction are recorded above.



## 2026-11-15 — final-pass microphone privacy correction

**Assistant outcome.** The request-by-request source review found a privacy gap: `SettingsValues.mIncognitoModeEnabled` included the global Always Incognito preference, but `mShowsVoiceInputKey` only mirrored the field-level gate. As a result, the footer could still show the mic and `LatinIME` could dispatch voice input in global incognito. The current source batch now makes mic visibility depend on `!mIncognitoModeEnabled` and gates the voice key event with that same computed flag. The branch is pushed to GitHub; signed CI and final device acceptance remain pending. The existing password and `IME_FLAG_NO_PERSONALIZED_LEARNING` guards remain intact.


## 2026-11-15 — English-only Dictate feedback and final audit continuation

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Assistant outcome.** Rechecked the user's issue [#359](https://github.com/DevEmperor/DictateKeyboard/issues/359), its maintainer's current response, and Dictate's current RecognitionService source. The service uses the saved Dictate Active language; it does not read the caller's recognition Intent and returns no language-detection metadata. brrBoard source commit [283b179](https://github.com/Batmancpu/brrrBoard/commit/283b17987d025fa2ca869df817755be37723eb4b) continues to request English, requests Android language detection on API 34+ for providers that implement it, refuses confidently tagged non-English or Devanagari final results, and updates the message with the exact Dictate setting. This is a guard, not an override of Dictate's private setting. The owner must set Dictate → Languages → Active language = English and disable Follow keyboard language; Reno10 acceptance remains open. Obtainium run [43776497322](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497322) and debug run [43776497364](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497364) were in progress. The request-by-request source audit continues; no tests were manually run.


## 2026-11-16 — Hindi Dictate report, Return/Delete icon batch, and retrospective

**User**

> Keep doing what you are doing. I just want to say when you are done and everything, the goal of this chart is reached, please take a look back at the whole chart and each thing and check if nothing is missed or nothing is ill-implemented. If anything is left, please implement that, okay? Keep continuing your work. All right, and also want to tell you the APK that you released. I believe it is finally being rooted through dictate. But it is only showing me Hindi transcriptions. Either it should show English or English only, but no Hindi, okay? And please continue the work that you were doing. Look at all the chat, look at the whole context and everything and please continue the work from here, right where you left

**Assistant outcome.** Continued the request-by-request source audit. Commit [6f308ac](https://github.com/Batmancpu/brrrBoard/commit/6f308ac72b3b14364d0ef0a98542ff8242e3c0d0) changes only `app/src/main/res/drawable/sym_keyboard_return_lxx.xml` and `sym_keyboard_delete_lxx.xml`. The existing default icon map in `Defaults.PREF_CUSTOM_ICON_NAMES` selects these resources. Signed release [v1.0.39](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.39) succeeded in Obtainium run [43778036994](https://github.com/Batmancpu/brrrBoard/actions/runs/43778036994), size 51,024,650 bytes, SHA-256 `4451b5cdaa653c95e27cca92f596f13d57fad971097337a049eba40495965865`. Debug run [43778036946](https://github.com/Batmancpu/brrrBoard/actions/runs/43778036946) built and signer-verified the debug APK, then failed its automatic test gate: 13/180 tests (5 ParserTest, 7 InputLogicTest, 1 StringUtilsTest). The failed cases cover currency/parser expectations, URL/autospace, and Emoji recognition. No tests were run manually.

The latest source retains the Dictate inline RecognitionService route and the English-result guard shipped in v1.0.38: English locale request, API 34+ language detection request, confident non-English tag rejection, Devanagari rejection, and a Dictate language-settings message. The maintainer response to [Dictate issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359) confirms this route keeps the keyboard visible and that Dictate's RecognitionService ignores the caller's language Intent. The owner must set Dictate → Languages → Active language = English and disable Follow keyboard language; Latin-script Hindi cannot be reliably classified without service metadata. The owner's Hindi report remains unresolved until Reno10 confirmation.

The full source audit found that the repository's Gemini-backed `AiWritingToolsView` contains a Proofread UI but is unreachable through the current AI_TOOLS input route; Android's current SpellCheckerService is word-level. The cloud proofreader was not activated under the offline typing requirements. GlideLib's archived upstream identifies its native library as proprietary Google code, has no LICENSE file, and labels the ARM64 build untested; it remains excluded from public APKs. The complete audit and source/device acceptance split are in [the active plan](POST_V1_0_19_RENO10_PLAN.md). All context, known issues, roadmap marker, README, and AGENTS instructions have been updated. Physical Reno10 acceptance remains the owner's.


## 2026-11-16 — owner supplied previous-chat context and authorized implementation

**Owner request**

> The typing library that I told you, I have tested on my phone. It works. It gives bad suggestions but that is my dictionary probably. That is not the typing library.
>
> Now look at this chart, look at all the other things and please fix this shit, this mess that we have created one by one.
>
> I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context.
>
> And then start doing it.

**New requirements and corrections.** Use the two attached iOS keyboard screenshots as references without publishing them. Limit keyboard layouts to English US/UK and Romanized Hinglish; remove Key Style/Icon Style selectors while preserving unrelated preferences; use two automatic OG Frosted Glass appearances plus optional action-key tint. Fix proportional layout, bottom/toolbar spacing, stable height, horizontal Emoji pages, visible Smart Emoji entry, Emoji-to-letters behavior, and the thin white top-edge highlight. Bundle the static personal and Emoji dictionary files recovered from the saved FrostKeys backup, but never bundle its learned UserHistoryDictionary. Remove the external gesture-library picker and aim for a bundled offline recognizer; the owner reports GlideLib works on-device, while its redistribution rights remain unresolved. Trace the reported offline mic behavior against the Android RecognitionService actually selected, preserve inline Dictate and English/privacy guards, and do not claim Reno10 acceptance from CI.

**Assistant plan and first checkpoint.** Work is authorized in reviewable batches: (0) update handoff, (1) language/style cleanup and failing-test triage, (2) geometry/tint/top-edge appearance, (3) Emoji/suggestion behavior, (4) static dictionaries and licensed bundled swipe support, (5) Dictate/provider diagnosis, privacy, build and device checklist. Every batch must update PROJECT_CONTEXT.md and this conversation record before the next. Raw screenshots, typed content, and learned history stay out of the public repo. See the active plan for exact source paths and acceptance criteria.


## 2026-11-16 — first implementation batch: languages and styles

**Assistant outcome.** Applied the first code batch from the authorized plan. `method.xml` now declares only `en-US`, `en-GB`, and `hi-Latn`; `LanguageScreen.kt` lists only these three choices; and `SubtypeSettings.kt` clears old additional languages once, enables the three choices on the first upgrade, and preserves later user language toggles. The appearance screen no longer exposes Key Style, Icon Style, individual icon customization, or theme-preset selectors. Existing default key and glyph resources are fixed in the renderer so old saved style preferences cannot bring the retired variants back.

Static validation: XML parsing found exactly three subtypes and `git diff --check` passed. Local `./gradlew :app:assembleDebug` stopped before compilation because it attempted to download Gradle 8.14.4 and the workspace returned `Network is unreachable`; no tests were run. The remote workflow is the next source-build check. Next code batch: horizontal Emoji paging, visible Smart Emoji access, ABC return behavior, and keyboard frame geometry.


## 2026-11-16 — subtype regression correction and Emoji batch

**Owner direction**

> Now give me an extremely detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything. I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context. Delete all these unnecessary languages and everything. And preferences appear in toolbar and everything. Make it iOS-like. Make it a proportionate, you know, a proportionate iOS keyboard button on my Android with everything there. And work on this smart emojis entry. I don't see it working. And this dictate button, when I click on it, it's still not routing to the dictate application but some offline keyboard. I'm pretty sure about this. So give me an extremely detailed plan on how you'll do in brief points, what you'll do, what you'll change, in how many batches. And then start doing it

**Outcome and correction.** The first language/style commit filtered `method.xml` to three subtype declarations, while also limiting the language screen and persisted enabled list. Obtainium run [43804893066](https://github.com/Batmancpu/brrrBoard/actions/runs/43804893066) built, signed and published v1.0.40. Debug run [43804893049](https://github.com/Batmancpu/brrrBoard/actions/runs/43804893049) built and signer-verified an APK but its automatic gate found 21/180 failures: 13 in the pre-existing baseline and eight caused by removing the internal locale fixtures (two Parser, three Subtype, three Hangul InputLogic). Restored the full internal catalog (128 subtype entries) while retaining the owner-only three-language settings list and first-run migration. This is a correction to the earlier Batch 1 note, which said the manifest itself had only three entries. v1.0.40 is not the device acceptance build.

The active Emoji code batch turns the page list horizontal with snap paging and category/page state, exposes a sparkle Smart Emoji icon/accessible label on the existing Recents category, changes the lower-left key to ABC and returns to letters, and removes the redundant top-strip back arrow. Source commit [d1f8aff](https://github.com/Batmancpu/brrrBoard/commit/d1f8affdb637266693b2a3c91f8d0ba380519d0b) is on main; automatic CI is pending. The GitHub tree assembly briefly created a zero-byte `GENTS.md` due to a first-path parsing error; it is being removed in a cleanup checkpoint. Do not publish screenshots or the old personal learning history.


## 2026-11-16 — resume from supplied prior chat and screenshots

**Owner's latest direction**

> The typing library that I told you, I have tested on my phone. It works. It gives bad suggestions but that is my dictionary probably. That is not the typing library. Okay. Now look at this chart, look at all the other things and please fix this shit, this mess that we have created one by one. Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything. I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context. Delete all these unnecessary languages and everything. And preferences appear in toolbar and everything. Make it iOS-like. Make it a proportionate, you know, a proportionate iOS keyboard button on my Android with everything there. And work on this smart emojis entry. I don't see it working. And this dictate button, when I click on it, it's still not routing to the dictate application but some offline keyboard. I'm pretty sure about this. So give me an extremely detailed plan on how you'll do in brief points, what you'll do, in how many batches. And then start doing it

**Carried-forward decisions.** Preserve the package/signature/update identity; keep only English US, English UK, and Romanized Hinglish in keyboard settings while retaining Android's internal subtype fixtures; remove obsolete key/icon style controls but keep unrelated preferences; use the two attached keyboard screenshots as visual references without copying them into the public repo. The supplied static dictionary and Emoji files are already bundled by default; learned history is not. Keep the three-slot suggestions and center auto-correct cue, make Smart Emoji discoverable, use horizontal Emoji paging with an ABC return, and preserve the user's Emoji/Clipboard/Settings hold menu. Keep typing offline, keep Dictate's RecognitionService route inline, and require the provider's Active language to be English for English output. Do not distribute the unlicensed GlideLib binary; continue finding a redistributable default swipe path.

**CI/source correction.** The first language batch altered the internal `method.xml` catalog and added eight regressions to the previous 13-test baseline; commit [d1f8aff](https://github.com/Batmancpu/brrrBoard/commit/d1f8affdb637266693b2a3c91f8d0ba380519d0b) restored all 128 internal subtypes. Obtainium run [43806306816](https://github.com/Batmancpu/brrrBoard/actions/runs/43806306816) published v1.0.41. Debug run [43806467695](https://github.com/Batmancpu/brrrBoard/actions/runs/43806467695) compiled, signer-verified and uploaded the APK; the automatic test job still reports the known 13/180 baseline failures (5 parser/currency, 7 URL/autospace, 1 Emoji recognition). No tests were manually run. A zero-byte `GENTS.md` created during tree assembly was removed in `9a86627`.

**Current source work.** The local Batch 2 diff updates `RoundedKeyboardFrameView.kt` to use only the visible navigation inset, avoid forwarding a second bottom inset into children, and draw a narrow white line along the rounded top of the frosted frame. `InputView.refreshBoardFrameBackground()` reapplies `MAIN_BACKGROUND` after the editor changes and theme reloads. `git diff --check` passed; this diff has not yet had remote compile/CI. The actual height, bottom margin and URL → text tint must be checked on Reno10; CI is not device proof.

**Assistant plan.** Implement in small checkpoints: (1) keyboard language/style cleanup and recover from the internal subtype regression; (2) stable keyboard frame, host boundary, visible nav inset, neutral tint reset, and top highlight; (3) horizontal Emoji pages, visible Smart Emoji, ABC return, then contextual Emoji candidates across the existing three suggestion slots; (4) confirm supplied static dictionaries/Emoji data and remove external gesture setup only when a redistributable swipe engine works by default; (5) verify Dictate service dispatch, privacy/editor actions, and produce a complete request-by-request source/CI/device audit. CI results and Reno10 acceptance stay explicitly separate.

## 2026-11-16 — current owner correction and CI compile recovery

**Latest owner clarification**

> So the the typing library that I told you, I have tested on my phone. It works. It gives bad suggestions but that is my dictionary probably. That is not the typing library. Okay. Now look at this chart, look at all the other things and please fix this shit, this mess that we have created one by one. Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything. I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context. Delete all these unnecessary languages and everything. And preferences appear in toolbar and everything. Make it iOS-like. Make it a proportionate, you know, a proportionate iOS keyboard button on my Android with everything there. And work on this smart emojis entry. I don't see it working. And this dictate button, when I click on it, it's still not routing to the dictate application but some offline keyboard. I'm pretty sure about this. So give me an extremely detailed plan how you'll do in brief points, what you'll change, in how many batches. And then start doing it

**Agent plan for the authorized work.** (1) Restore a compiling source checkpoint and inspect automatic CI. (2) Compare measured frame, panel, footer, key-row and editor boundaries, then fix only the source-proven height/spacing defects. (3) Complete the iOS-like Emoji navigation and contextual three-slot suggestions while preserving the decoder's existing center auto-correct semantics. (4) Keep only requested language choices and default appearance controls, correct dark/light action-key tint and icons, and preserve unrelated preferences. (5) Make swipe typing work by default using a redistribution-compatible offline engine; treat the owner's GlideLib test as confirmation of phone operation but do not put the unlicensed binary in the public APK. (6) Verify the selected Dictate RecognitionService path, keep typing/emoji personalization local, and record device-only acceptance checks. Each step is a GitHub checkpoint with updated AGENTS.md, PROJECT_CONTEXT.md, this plan, and this conversation record.

**CI failure and source fix.** The previous frame/inset commit [a271e5a](https://github.com/Batmancpu/brrrBoard/commit/a271e5a63c5584c9e817919cf6ab9d9b70ecabba) applied visible navigation insets, a 1dp white rounded top highlight, and a theme surface refresh on editor changes. Automatic release run [43807891600](https://github.com/Batmancpu/brrrBoard/actions/runs/43807891600) and debug run [43807891534](https://github.com/Batmancpu/brrrBoard/actions/runs/43807891534) both failed Java compilation: `LatinIME.java:1318` called `refreshBoardFrameBackground()` on `mInputView`, declared as `View`. The local source now narrows it with `mInputView instanceof InputView inputView` before calling the method. This is a targeted compiler correction; no local test was run. Push and wait for the next automatic CI result before moving to another feature batch.

**Current verification boundary.** The latest signed release is still v1.0.41. CI has not yet built the local compiler correction. Source shows the Smart Emoji sparkle icon and accessibility label; physical discoverability and strip behavior have not been verified on Reno10. The owner reports GlideLib works on their phone; its public archive lacks redistribution terms, so the build must use a licensed default alternative or obtain an explicit redistributable upstream license. No public Apple emoji font is added; retain the already-bundled static dictionary/Emoji data and local system emoji rendering. Dictate's RecognitionService route exists in source but its provider selection/output remains a device acceptance item.

## 2026-11-16 — compiler recovery and Dictate service selection

**CI correction.** Source commit [935d0e6](https://github.com/Batmancpu/brrrBoard/commit/935d0e6be00904531d2871cb6dc2e0271849973f) adds the `instanceof InputView` guard for the frame refresh call in `LatinIME.java`. Obtainium run [43808815142](https://github.com/Batmancpu/brrrBoard/actions/runs/43808815142) succeeded and published [v1.0.43](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.43). Debug run [43808815131](https://github.com/Batmancpu/brrrBoard/actions/runs/43808815131) built, verified signer identity and uploaded the APK; its automatic test step was still running. No manual test/build was run.

**Dictate route finding.** The user reports that mic taps still reach an offline recognizer. `InlineDictation.kt` calls Android's default `SpeechRecognizer.createSpeechRecognizer(context)`, which delegates to the Android system-selected RecognitionService; installing Dictate as a keyboard does not select it as the voice service. Dictate's maintainer on [issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359) explains the difference: RecognitionService leaves the current keyboard visible, the voice-input IME replaces it, and `ACTION_RECOGNIZE_SPEECH` opens an activity. Dictate currently publishes `net.devemperor.dictate/dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService` in its manifest. Android exposes `Settings.ACTION_VOICE_INPUT_SETTINGS` for this choice and a component-specific `SpeechRecognizer` API; keep the standard system-default route so the OS's selected provider remains authoritative.

**Current code batch.** `InlineDictation.kt` now checks `Settings.Secure` key `voice_recognition_service` before recording. If that component is not Dictate's RecognitionService, it gives a clear message and attempts to open Android Voice input settings, then returns without silently using ColorOS's or another provider's offline engine. If Settings does not expose that screen, it shows the manual fallback. It does not start the voice-input IME or `ACTION_RECOGNIZE_SPEECH`. `strings.xml` adds the two setup messages. This behavior still needs CI and Reno10 verification; after selecting Dictate, the Dictate app's Active language must be English and Follow keyboard language disabled for the existing English-only request/filter to work.

**Next.** Check CI for the Dictate selection guard, then continue panel geometry/footer measurements, contextual three-slot Emoji suggestions, and a licensed swipe engine assessment. The full request-by-request device acceptance remains open.

## 2026-11-16 — Smart Emoji navigation recovery

**Owner request.** The owner repeated that the personal swipe library has been verified on their phone, asked for the actual iOS-like layout, dictionary/Emoji integration, removal of extra language choices and style controls, working Smart Emoji, Dictate routing, and detailed staged implementation. This request is already quoted in full above; this checkpoint records its current status.

**Correction after a malformed GitHub checkpoint.** Commit [f46dd58](https://github.com/Batmancpu/brrrBoard/commit/f46dd586423a85832095990f813de134891986ba) incorrectly stored `EmojiPalettesView.java`, `KeyboardSwitcher.java`, and this conversation file as binary data. Obtainium run [43810986135](https://github.com/Batmancpu/brrrBoard/actions/runs/43810986135) and debug run [43810986168](https://github.com/Batmancpu/brrrBoard/actions/runs/43810986168) both failed during source compilation with unresolved `KeyboardSwitcher` references because the Java source blob was binary; the Emoji navigation change was not validated. Corrective commit [7d0f191](https://github.com/Batmancpu/brrrBoard/commit/7d0f191a36a5f9750539e71b49eb7ab1e3e901d8) restored valid UTF-8 files. Follow-up source commit [1d45dee](https://github.com/Batmancpu/brrrBoard/commit/1d45deeca4d3dd9e1dbea425ce33ce3c82e70fc5) hides stale typing candidates before revealing the shared Emoji category strip. The repaired patch always builds category tabs, shows the category strip while Emoji is active, restores it after search, and subtracts its height from the Emoji grid when the typing toolbar is disabled. The malformed commit remains in history; do not build from it.

**Verification state.** Corrected runs are [43811864334](https://github.com/Batmancpu/brrrBoard/actions/runs/43811864334) (Obtainium release pending) and [43811864409](https://github.com/Batmancpu/brrrBoard/actions/runs/43811864409) (debug build in progress). The corrective tree is verified as UTF-8 and `git diff --check` passed; no tests were run manually. Reno10 Smart Emoji discoverability/height still needs on-device confirmation.

**Next.** Wait for both corrected workflows. If the source build passes, continue with the measured panel/footer geometry batch. Keep the 13/180 automatic-test baseline separate from compile/build status, do not call the APK phone-accepted, and do not run manual tests.

## 2026-11-16 — corrected Smart Emoji release and footer geometry batch

**Owner direction carried forward.** The owner re-supplied the prior-chat context and the two iOS keyboard reference screenshots, clarified that GlideLib works on their Reno10 but its suggestions were poor, and repeated: “Now give me an extremely detailed plan how you'll do in brief points, what you'll change, in how many batches. And then start doing it”. The requested order remains layout/interaction, appearance and icons, Emoji/suggestions, local dictionary/swipe support, Dictate, and a complete handoff audit. Do not copy the screenshots or their message text to this public repository. The visible reference shows a three-slot strip, center-emphasized candidate, compact accessory at right, QWERTY rows, and Emoji/mic at opposite lower corners; this does not verify hidden gestures or exact dimensions.

**Corrected Smart Emoji checkpoint.** Obtainium run [43811864334](https://github.com/Batmancpu/brrrBoard/actions/runs/43811864334) published [v1.0.47](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.47), source commit [1d45dee](https://github.com/Batmancpu/brrrBoard/commit/1d45deeca4d3dd9e1dbea425ce33ce3c82e70fc5), SHA-256 `69260e996aed8ee61bc9ce4db645c86c745a699ebf74222652cf9c698f036cf1`, 51,028,859 bytes. Emoji category navigation is built and shown while Emoji is active even if typing suggestions are globally hidden; the category row's height is reserved in the Emoji grid, and stale typing candidates are hidden before the shared strip is revealed. The debug workflow [43811864409](https://github.com/Batmancpu/brrrBoard/actions/runs/43811864409) compiled, signer-verified and uploaded the APK, then reported the known 13/180 automatic test failures (5 ParserTest, 7 InputLogicTest, 1 StringUtilsTest). No tests were run manually. Reno10 discoverability and panel height remain owner checks.

**Current implementation — footer balance.** The source patch changes `InputView.onMeasure()` to retain the footer for all `KeyboardId.isNumberLayout()` modes as well as alpha/symbol keyboards, so numeric and phone layouts do not omit the lower controls. `main_keyboard_frame.xml` now uses a 48dp footer and gives the Emoji/mic `ImageButton`s a 10dp bottom content inset. Their hit boxes remain 48dp; the drawable centers at the old 38dp footer's vertical center, lifting the visible glyph and increasing its lower breathing room. `config.xml` names these dimensions and the compensating amount. `ResourceUtils.getKeyboardHeight()` subtracts that same 10dp from the keyboard body, so body plus footer is intended to remain the previous total height across the normal size scale. This is arithmetic from layout dimensions, not phone measurement. Automatic source build and Reno10 acceptance remain pending for this checkpoint.

**Plan and next action.** Keep the seven ordered batches in `docs/POST_V1_0_19_RENO10_PLAN.md`: carry-forward/CI baseline; language and settings cleanup; frame/insets/tint/footer; Emoji navigation and three-slot suggestions; finite brrPersonal plus licensed default swipe engine; Dictate/privacy; signed release and request-by-request acceptance. Footer changes are now in signed v1.0.48. Continue with the contextual suggestion source checkpoint, then inspect its CI before tint/alternate-key work. Keep Reno10 checks explicitly unverified. No manual tests were run for the footer batch.

## 2026-11-16 — v1.0.48 footer and contextual Emoji source checkpoint

**Footer release.** Source commit [96145b2](https://github.com/Batmancpu/brrrBoard/commit/96145b20a0a844038aac2cc9f38e4595ed0bc8d6) was signed and published as [v1.0.48](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.48) by Obtainium run [43814228255](https://github.com/Batmancpu/brrrBoard/actions/runs/43814228255). The footer remains present in number/phone layouts, uses 48dp footer and hit regions, and has a 10dp optical inset balanced by a 10dp key-body compensation. Debug run [43814228332](https://github.com/Batmancpu/brrrBoard/actions/runs/43814228332) compiled, signer-verified and uploaded the APK; its automatic test gate reported the known 13/180 failures (5 ParserTest, 7 InputLogicTest, 1 StringUtilsTest). No tests were run manually. Reno10 measurements and acceptance remain open.

**Contextual Emoji source patch.** `DictionaryFacilitatorImpl.kt` now keeps at least one ordinary text result for a non-empty typed token while allowing its existing decoder-ranked Emoji results to remain in the two side slots. `Suggest.kt` keeps the first text result centered if an Emoji ranked ahead of it; the center's `mWillAutoCorrect` flag and decoder scores are unchanged. Empty-token and batch/swipe suggestion ordering retain the prior two-text preference. The brrPersonal valid-only filter still removes text suggestions for its slang/profanity while permitting Emoji. This patch exposes Emoji already returned by the active token dictionaries; it does not yet blend local Emoji recency into the text strip. Source/CI checkpoint pending; no manual tests run.

**Next.** After CI, confirm the three visible strip positions behave and retain explicit tap-to-commit. Then finish the local Emoji ranking connection, tint and key-interaction audit, and licensed swipe-engine path. Do not infer visual spacing or relevance acceptance from build results.


## 2026-11-16 — v1.0.49 strip and Dictate routing

**Owner request (verbatim excerpt).**

> “Now i am saying to you:- So the the typing library that I told you, I have tested on my phone. It works. It gives bad suggestions but that is my dictionary probably. That is not the typing library. Okay. Now look at this chart, look at all the other things and please fix this shit, this mess that we have created one by one. Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything. I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context. Delete all these unnecessary languages and everything. And preferences appear in toolbar and everything. Make it iOS-like. Make it a proportionate, you know, a proportionate iOS keyboard button on my Android with everything there. Okay? And work on this smart emojis entry. I don't see it working. And this smart dictionary and everything. And this dictate button, when I click on it, it's still not routing to the dictate application but some offline keyboard. I'm pretty sure about this. So give me an extremely detailed plan on how you'll do in brief points, what you'll do, what you'll change, in how many batches. And then start doing it”

**Audit result.** Current Language & Layout source filters choices to English US, English UK and Romanized Hinglish; Emoji pages use horizontal snapping; the footer returns to letters as ABC; Appearance does not expose global key/icon style selectors. Internal locale catalogs remain intact for parser compatibility. These findings should be checked against the owner's phone when available.

**Release evidence.** [v1.0.49](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.49) was published from `b4faaaac2e5afafcdafb8f4f4a6d1df782b5ef5c` by Obtainium run [43815989435](https://github.com/Batmancpu/brrrBoard/actions/runs/43815989435). Debug run [43815989319](https://github.com/Batmancpu/brrrBoard/actions/runs/43815989319) built and uploaded a signer-verified APK, then failed the automatic gate: 180 tests completed, 13 failed. No manual tests were run.

**Implementation in this checkpoint.** `InlineDictation.kt` now resolves the selected `voice_recognition_service`, verifies the selected component is a visible Dictate `RecognitionService`, and constructs `SpeechRecognizer` with that explicit component. This prevents a silent fallback to another recognizer, keeps the brrBoard text keyboard visible, and does not launch Dictate's voice-input IME or recognition activity. The debug compile/test and Obtainium workflows are pending. Reno10 service selection, provider output and English-only behavior are not yet verified.

**Next.** Review CI, then fix the visual tint and alternate-key interaction, connect local Emoji usage counts to suggestions, choose a redistributable offline swipe engine, and complete the full requirement-by-requirement device checklist. Do not bundle the supplied Google-derived GlideLib without redistribution rights; keep the Apple Color Emoji font out of the public APK unless its redistribution rights are established.

## 2026-11-16 — v1.0.50 and neutral frosted defaults

**Dictate implementation and verification.** `InlineDictation.kt` now binds to the selected Dictate RecognitionService component after checking Android's configured component and service availability. The release workflow [43818042870](https://github.com/Batmancpu/brrrBoard/actions/runs/43818042870) published [v1.0.50](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.50) from [11810ad](https://github.com/Batmancpu/brrrBoard/commit/11810ad0e6778a2a473dbfc005dcc9a54630f131). Debug run [43818042687](https://github.com/Batmancpu/brrrBoard/actions/runs/43818042687) compiled, signer-verified and uploaded the APK, then failed the automatic gate at 13/180. Exact recurring failures: 5 parser/currency, 7 URL/autospace, and `isEmojiDetectsAllAvailableEmojis`; no manual tests were run.

**Tint implementation.** `Defaults.kt` changes frosted saturation/vibrancy defaults from values as high as 500% to 100%. `KeyboardTheme.kt` migrates stored preferences only if they exactly match prior shipped defaults; custom settings remain. Optional action tint retains the wallpaper/system accent-to-Apple-palette mapping. This batch's build and visual result are pending.

**Next.** Inspect CI, then audit alternate-symbol popups and visible icons, connect local Emoji counts to the strip, and choose the default swipe engine with its Hinglish lexicon and redistribution terms verified. Reno10 Dictate and color behavior need owner confirmation.

## 2026-11-16 — v1.0.51 neutral tint result and handoff

**Latest owner direction.** The owner repeated the previous-chat requirements and explicitly asked for the ordered plan and implementation. The request is already quoted verbatim in the immediately preceding owner checkpoint above; no new product constraint supersedes it. Keep the screenshot-derived target and all open Reno10 checks.

**Tint result.** Source commit [3cbdab6](https://github.com/Batmancpu/brrrBoard/commit/3cbdab649b7df1fa6efc39d5f9f0e788143e642c) sets frosted saturation/vibrancy defaults to 100% and migrates only stored values that equal the old defaults. Obtainium run [43819288817](https://github.com/Batmancpu/brrrBoard/actions/runs/43819288817) published [v1.0.51](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.51), 51,028,859 bytes, SHA-256 `f52c1ea1031ec20720abc4595f5899495054e4b1710a2f67b2504cdaf4b6ef97`. The debug workflow [43819288789](https://github.com/Batmancpu/brrrBoard/actions/runs/43819288789) compiled, verified signing and uploaded a 59,266,617-byte APK (SHA-256 `453c9ed21a0448f7f0674283c3e245ff6a61bef7300f265fa0403418b210bbd7`), then failed its automatic gate at the unchanged 13/180 baseline: five parser/currency, seven URL/autospace, and one Emoji-recognition failure. No tests were run manually.

**Acceptance boundary and next action.** Release CI passing confirms a signed APK was produced; it does not verify appearance on the Reno10. Next audit alternate-key popup and visible icon behavior, connect local Emoji use/recency and brrPersonal shortcuts to the suggestion strip, and finish the rights-cleared default swipe path. Maintain the four-file handoff and leave Reno10 color, geometry, Dictate provider and transcription acceptance open.


## 2026-11-16 — selected Emoji highlight palette correction

**Source finding.** `EmojiPalettesView.setCurrentCategoryId()` colored the active category with raw Android `colorAccent`, while keyboard action keys already used the shared CIELAB nearest-Apple palette mapper. That left an inconsistent accent in the Emoji category strip.

**Change and expectation.** `KeyboardTheme.kt` exposes the existing palette mapper as a static method; `EmojiPalettesView.java` routes the selected category highlight through it. This changes only the selected Emoji tab highlight. The keyboard glass and letter/action key colors are unchanged in this batch. The four handoff files are updated in the same source checkpoint. CI and Reno10 visual acceptance are pending.

**Next action.** Wait for the signed/debug workflows, then continue the alternate-symbol popup/icon audit, local Emoji ranking, and rights-cleared swipe-engine assessment.


## 2026-11-16 — v1.0.52 Apple-palette result and payload audit

**Owner direction carried forward.**

> “So give me an extremely detailed plan on how you'll do in brief points, what you'll change, in how many batches. And then start doing it”

The required order is implementation in small batches, with a concrete plan and all progress retained in the GitHub handoff for the next agent. Preserve the iOS-inspired layout target, three owner-facing languages, neutral Apple palette, contextual Emoji, bundled offline Personal data, a rights-cleared swipe engine, selected Dictate service, and Reno10-specific acceptance boundary. No plan-only pause is requested.

**v1.0.52 result.** Obtainium run [43820853475](https://github.com/Batmancpu/brrrBoard/actions/runs/43820853475) published [v1.0.52](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.52) from [34c4179](https://github.com/Batmancpu/brrrBoard/commit/34c4179dc1a867616f474f26d5b5d0df3adc910c), APK 51,028,859 bytes, SHA-256 `c1b8063b9d74abf86fd423d2eb967a74b87b5b266635372cd6408de0d7f570ab`. The selected Emoji category highlight now uses the nearest Apple-palette mapper. Debug run [43820853163](https://github.com/Batmancpu/brrrBoard/actions/runs/43820853163) built, signer-verified and uploaded an APK, then failed the automatic gate at 13/180 (5 parser/currency, 7 URL/autospace, 1 Emoji-recognition). No manual tests were run.

**Next source batch — trim installable dictionary assets.** Repository assets contain 21 dictionaries. Keep the three owner-facing main dictionaries (`main_en-US.dict`, `main_en-GB.dict`, `main_hi-Latn.dict`) and both current Emoji dictionaries. Exclude the other 16 main dictionaries, 30,876,181 raw bytes, from installable build variants through Gradle's asset-ignore list. Leave all source files in place and retain the complete catalog in the `runTests` build type; this trims APK payload without deleting test fixtures or dictionary files from history. The existing selected-language UI and internal subtype catalog remain unchanged. CI will establish actual compressed APK savings and ensure asset lookup still works.

**Next after this batch.** Continue the audit of alternate-symbol popups and visible icons/layout proportions; wire local Emoji usage/recency and brrPersonal shortcuts into the three-slot suggestion strip; then validate a default swipe decoder with clear redistribution rights. Keep device-only acceptance open for Reno10 geometry, palette rendering, Dictate component/language and swipe accuracy.


## 2026-11-16 — v1.0.53 dictionary APK trim and Smart Emoji source audit

**Owner direction carried forward.**

> “Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything.”

Continue in reviewable implementation batches and preserve all progress in the four-file GitHub handoff. Keep the iOS-inspired layout target, three visible owner languages, neutral Apple palette, contextual Emoji, bundled offline Personal data, default rights-cleared swipe typing, selected Dictate service, and Reno10-specific acceptance boundary.

**Dictionary payload result.** [v1.0.53](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.53), commit [1d9ed8d](https://github.com/Batmancpu/brrrBoard/commit/1d9ed8d476870e3fd0918659b0098ab86a72af4f), was built and signed by Obtainium run [43822092860](https://github.com/Batmancpu/brrrBoard/actions/runs/43822092860). Release APK: 37,949,189 bytes, SHA-256 `38817186ea3db60c2937f7afa28ab89e49dff0e6163e351c71d21b8df5be62c6`; down 13,079,670 bytes (~25.6%) from v1.0.52. Debug run [43822092903](https://github.com/Batmancpu/brrrBoard/actions/runs/43822092903) built/signer-verified/uploaded its APK (46,063,050 bytes, SHA-256 `d7cb84cd406b1d07c5c95b9f708e83c51a956966c4a112928588a11aa40eac5b`) and completed 180 tests with the same 13 baseline failures (5 parser/currency, 7 URL/autospace, 1 Emoji recognition). No manual tests were run.

**Payload change.** All installable build variants now exclude 16 unsupported main dictionary assets totaling 30,876,181 raw bytes. English US, English UK, Romanized Hinglish and both bundled Emoji dictionaries remain eligible; source files and the full test catalog remain intact. The release workflow's exact-byte checks for shipped Hinglish and Emoji dictionaries passed. The APK shrank 13.08 MB compressed.

**Smart Emoji source finding.** The Smart tab already returns `ic_emoji_smart`, has a Smart accessibility label, and its tab strip is made visible when Emoji mode opens. The optional persistent Emoji row is disabled by default and separate from the Emoji panel. The existing local frequency/recency ranker feeds that optional row; next integrate it into Smart category contents and typed-word side suggestions while preserving the text candidate and decoder scores. Reno10 visibility and relevance still require owner confirmation.


## 2026-11-16 — Smart Emoji first-use implementation

**Owner (verbatim, current direction)**

> “Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything.”
>
> “And then start doing it”

The owner also supplied two visual keyboard frames. They show a dark iPhone text-entry surface with a three-slot candidate strip, a compact control at the strip's right, a QWERTY layout, and Emoji/mic controls at opposite ends of the bottom row. The frames are visual references only; they do not prove gesture, prediction, or Dictate behavior. Message text, names, and media details are intentionally omitted from this public record.

**Source audit and correction.** The Recents tab already uses the sparkle Smart Emoji glyph, and EmojiPalettesView.startEmojiPalettes() already calls the local AdaptiveEmojiEngine and moves matching Recents into ranked order. The first-use defect was that the stored Recents list is empty on install; no keys existed to reorder, and EmojiCategory.initialize() fell back to Smileys. An earlier plan note that implied the ranker only fed an optional row was inaccurate and is superseded by this source trace.

**Implementation in this batch.** Seed the ranked local Emoji list into free Recents-grid capacity when the panel is initialized, select Smart Emoji for a new install and migrate the old empty-Recents-to-Smileys fallback once, and keep those transient starter entries out of persisted user Recents. Preserve user recents when the grid is full; limit ranking to the smaller of the grid capacity and three rows. Skip ranking while incognito. CI and Reno10 acceptance are pending at this handoff.

**Previous CI failure verified.** Debug run [43823427430](https://github.com/Batmancpu/brrrBoard/actions/runs/43823427430) compiled the debug APK, verified its signer, uploaded it, and ran 180 tests. The workflow failed only at its explicit test gate because the same 13 known failures remain (five Parser/currency, seven InputLogic URL/autospace, one emoji recognition). The APK build itself succeeded. No Reno10 manual test was performed in that run.


### Smart Emoji source batch — CI and release result

Source commit [8caeb5d](https://github.com/Batmancpu/brrrBoard/commit/8caeb5d963b76a606f636ad9c0e08236bacf9648) populated the first-use Smart Emoji/Recents grid from the existing local ranker, migrated the old Smileys fallback to Smart once, limited starter Emoji to unused capacity, kept transient starters out of saved Recents, and skipped adaptive ranking in incognito mode. It updated the four handoff files in the same source commit.

Obtainium run [43825284915](https://github.com/Batmancpu/brrrBoard/actions/runs/43825284915) passed and published [v1.0.54](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.54). APK size is 37,949,189 bytes; SHA-256 is 408a9f60298a88bcf7b4c1a94db4a883808dbfc9d0b65944c852ab73d738046d. Debug run [43825284952](https://github.com/Batmancpu/brrrBoard/actions/runs/43825284952) compiled, signer-verified, and uploaded artifact 14206735867 (34,844,049 bytes); APK SHA-256 is eb1eb2da1b92cb71e06c4cbb36ea1480055297f87cf64552914343ec321ee224. The test task completed 180 tests with the known 13 failures: five parser/currency, seven URL/autospace, and one Emoji recognition. The change introduced no new test failure. Reno10 visual/interaction acceptance was not run. Next source batch: rank only the left/right Emoji suggestions from local Emoji usage, preserving the centered word candidate and decoder confidence.

### Contextual Emoji side-ranking source batch

The owner wants the two outer suggestion positions to use relevant Emoji while the center remains the word candidate and receives the existing confidence fill only when the decoder sets mWillAutoCorrect. Source files in this batch are SuggestionStripView.kt and SuggestionStripLayoutHelper.java. The display layer uses the existing cached local Emoji frequency/recency ranking to move only Emoji that the decoder or dictionary already supplied into the visible side slots. It clones the suggestion list, preserves the center item, auto-correction state, decoder scores, and InputLogic's original list, and skips local ranking in incognito mode. It does not generate unsupported Emoji or add a new score formula. Source CI and Reno10 screenshot/interaction acceptance are pending at handoff.


### Contextual Emoji side-ranking — CI and reference research

Source commit [32d580d](https://github.com/Batmancpu/brrrBoard/commit/32d580dc276cd87e2aef42d707bdcf556b6d530a) moved locally ranked decoder-provided Emoji into the two side slots using a display copy; the center suggestion, mWillAutoCorrect, decoder scores, and original InputLogic model are preserved. Incognito does not read the Emoji ranker. Obtainium run [43827417372](https://github.com/Batmancpu/brrrBoard/actions/runs/43827417372) passed and published [v1.0.55](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.55), APK 37,949,189 bytes, SHA-256 70175b44c55653b8f8387c94f9691aa6dab837ce2cf5306c9d834c7055478693. Debug run [43827417364](https://github.com/Batmancpu/brrrBoard/actions/runs/43827417364) built, signer-verified, and uploaded artifact 14207641163 (34,844,224 bytes; APK SHA-256 6f1716cbe3b0289d3c13e28e14eb30c38860616d781610a4244c0939ef36fda6). Its gate completed 180 tests with the same known 13 failures: five parser/currency, seven URL/autospace, one Emoji recognition. No new failures were introduced, and no device test was run.

**Official iOS 27 reference checked:** Apple’s onscreen keyboard guide says touch-and-hold a related letter/number/symbol to choose alternate characters, and touch-and-hold Emoji or Switch Keyboard to open Keyboard Settings: https://support.apple.com/guide/iphone/type-with-the-onscreen-keyboard-iph3c50f96e/27/ios/27. Apple’s Emoji guide says tapping a suggested Emoji replaces the typed term: https://support.apple.com/guide/iphone/add-emoji-memoji-and-stickers-iph69df21ec5/27/ios/27. The two supplied frames show the three-slot text strip, small right accessory, QWERTY keys, and Emoji/mic at opposite bottom corners; they do not verify gestures or exact phone geometry. Next: audit alternate-character popups and the key-by-key behavior against those sources, then measure on Reno10.


### 2026-11-16 — owner resumed the full implementation request

The owner supplied the prior conversation and two iPhone keyboard reference screenshots, then requested that implementation begin in batches. The screenshots are used as visual evidence only; visible message text and raw image media are not copied into this public log.

> “Now i am saying to you:- So the the typing library that I told you, I have tested on my phone. It works. It gives bad suggestions but that is my dictionary probably. That is not the typing library. Okay. Now look at this chart, look at all the other things and please fix this shit, this mess that we have created one by one. Now give me a detailed plan what you will do after reading all this context, okay? I guess you should start with the basic corrections, adding the emojis, the glide typing library, the layout, behavior and everything. I still see layout buttons are not how they are in my iPad. And I still see the ugly color intent. And please add all this to the GitHub's carry forwarded context. Delete all these unnecessary languages and everything. And preferences appear in toolbar and everything. Make it iOS-like. Make it a proportionate, you know, a proportionate iOS keyboard button on my Android with everything there. Okay? And work on this smart emojis entry. I don't see it working. And this dictate button, when I click on it, it's still not routing to the dictate application but some offline keyboard. I'm pretty sure about this. So give me an extremely detailed plan on how you'll do in brief points, what you'll do, what changes, in how many batches. And then start doing it.”

**Source batch recorded with this handoff:** In `PointerTracker.java`, removed the custom `mLongPressAlpha123` interception that opened the enlarged numpad and blocked slide selection. A 123 tap still enters persistent symbols; sliding from that mode key can select one symbol, and `KeyboardState` restores letters when sliding input finishes. Standard character alternate popups remain in the existing popup system. Automatic workflows are pending for the source commit; there is no Reno10 acceptance result yet. Next is CI review, then the full glyph/popup map and measured Reno10 layout pass, followed by a rights-cleared default swipe decoder and Dictate/device acceptance.


### 2026-11-16 — 123 symbol-slide source batch and CI

Source commit [8463fc8](https://github.com/Batmancpu/brrrBoard/commit/8463fc80c1f8695c0f0b11075db25d5e0d3434db) removes the custom held-123 enlarged-numpad branch and its drag-suppression flag in `app/src/main/java/helium314/keyboard/keyboard/PointerTracker.java`. Tap 123 still switches persistently to Symbols; slide from 123 can select one symbol and the existing `KeyboardState` momentary transition returns to letters when sliding input finishes. Ordinary alternate-key popup code is unchanged.

- Obtainium run [43829494372](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494372) succeeded and published [v1.0.56](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.56); APK size 37,949,189 bytes, SHA-256 `1340be9ef8ba3b85cc080900d74d743b072c8ccd8b99d10d846de227ef276f25`.
- Debug run [43829494384](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494384) built the APK, verified signer identity, and uploaded artifact [14208306869](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494384/artifacts/14208306869); APK size 46,063,050 bytes, SHA-256 `3599b46374b0a06b83a548e1994e5360ed2d52d3a0a89ee85303b2c16c2db4dc`.
- The automatic test task ran 180 tests and failed 13: five Parser/currency, seven InputLogic URL/autospace, and one StringUtils Emoji recognition. These are the same known baseline; the new change introduced no extra failure. Debug publication was skipped because the gate failed. No manual tests or Reno10 test were run.
- The signed Obtainium APK is published for owner testing. The next source batch audits every visible key popup/icon, then uses Reno10 measurements for geometry. Rights-cleared default swipe engine, brrPersonal typing quality, Dictate output, and device acceptance remain open.

### 2026-11-16 — owner confirms full implementation authorization and priorities

The owner provided two still frames from an iOS keyboard comparison and authorized implementation in ordered batches. They require the carried-forward context files to be updated with every batch so another agent can resume without the failed work chat.

Key requirements to retain:

- Tailor brrBoard for the owner's Reno10 and tablet use. Keep visible languages to English US, English UK and Romanized Hinglish; remove dead feature choices and duplicate style/theme options while preserving useful settings.
- Match the three-slot suggestion strip, confident/highlighted center candidate, separate utility control, proportional key rows/footer, Emoji and microphone corners, stable panel height, horizontal Emoji browsing, and one natural ABC return behavior. Measure before claiming parity; the supplied attachment shows only two still frames, not the full Emoji panel or gesture flow.
- Keep automatic dark/light appearance, neutral keys, and a single optional restrained wallpaper-derived action tint. Fix stale editor tint on repeated field transitions and add the requested thin top-edge glass highlight.
- Make the mic use the configured Android Dictate RecognitionService rather than launching an unrelated keyboard/dictation screen. Device provider language and on-device model configuration need Reno10 confirmation.
- Keep a finite, fast, offline personal Hinglish vocabulary and local learning, with requested slang accepted without text suggestions but available to the Emoji ranker. Make Smart Emoji useful in both its category and contextual suggestions.
- The user confirms GlideLib works on their own phone, but the supplied library archive is not redistribution-cleared. The public APK must not bundle it without rights. Find a license-cleared offline engine and bundle it by default before removing the outside installer; keep this explicit blocker in handoff.
- Do not bundle the 116 MB Apple Emoji font because the upstream license excludes the artwork and it would inflate the APK. Existing static Hinglish/Emoji dictionaries already ship.
- Keep all future source/release changes and workflow outcomes in `AGENTS.md`, `PROJECT_CONTEXT.md`, `docs/POST_V1_0_19_RENO10_PLAN.md`, and this conversation log in the same batch commit. Preserve package/signing/update identity.

The current source batch hides dead AI Tools/GIF/Stickers and internal Close History toolbar choices, migrates old toolbar settings, and preserves working toolbar actions. CI is pending. Earlier shipped work: Smart Emoji first-use grid (v1.0.54), local Emoji side ranking (v1.0.55), and 123 slide-to-symbol behavior (v1.0.56). The v1.0.56 automatic test job had the unchanged 13/180 failures (five Parser/currency, seven InputLogic URL/autospace, one StringUtils Emoji recognition). No Reno10 acceptance has occurred.

Next after CI checkpoint: map visible icons/popups and implement measured geometry/interaction fixes; then Dictate routing, bundled rights-cleared swipe engine + brrPersonal quality, payload audit, and final device acceptance.

## 2026-11-16 — architecture mission, R0

Owner: “During this phase, you must not: … Implement keyboard features.” Owner: “Your primary deliverable is an exceptionally thorough, permanently preserved engineering knowledge base inside the brrBoard GitHub repository.” This session is documentation-only. The current source, release and CI anchors are in [R0](ENGINEERING_HANDOFF.md#r0--repository-baseline-on-16-november-2026). Continue with R1 source and CI triage. The supplied message ends in F6; missing text remains unverified.

## 2026-11-16 — R1 source architecture and test triage

[SOURCE_ARCHITECTURE_AUDIT.md](SOURCE_ARCHITECTURE_AUDIT.md). R2 will measure the visual reference and specify shared geometry. Future implementation begins with the test baseline.

## 2026-11-16 — R2 Reno10 and iOS visual reference audit

[RENO10_VISUAL_SPEC.md](RENO10_VISUAL_SPEC.md). Original media was sampled where available. R3 will trace the local ranking and swipe engine before choosing evaluation gates.

## 2026-11-16 — R3 local language engine, Smart Emoji and glide research

[LOCAL_TYPING_ENGINE.md](LOCAL_TYPING_ENGINE.md). Current Smart is usage-ranked Recents and the default build lacks a working gesture factory. R4 will trace provider integrations and privacy boundaries.

## 2026-11-16 — R4 Clipboard, inline autofill and Dictate integration audit

[IME_PROVIDER_INTEGRATIONS.md](IME_PROVIDER_INTEGRATIONS.md). The mic-to-settings branch and Clipboard safety seams are mapped. R5 will order test repair, editor safety, interaction and late release gates.

## 2026-11-16 — R5 dependency-ordered implementation master plan

[MASTER_IMPLEMENTATION_PLAN.md](MASTER_IMPLEMENTATION_PLAN.md). The plan is ready for a later implementation session. R6 will verify links, source immutability, recovered history and the remote checkpoint chain.

## 2026-11-16 — R6 handoff verified

[Engineering handoff verification](ENGINEERING_HANDOFF.md#r6--verification-and-readiness). The documentation-only architecture phase is complete. A later authorized implementation should begin with B00 and the current CI triage before changing product features.

## 2026-11-16 — B00 implementation baseline

The owner superseded the earlier planning-only scope and authorized the full dependency-ordered implementation while unavailable for approximately 4–5 hours. Verbatim direction: “Start with Batch B00 and proceed through the dependency-ordered implementation plan.” “Do not repeatedly stop to ask me whether you should continue.” “Do not push broken, partially implemented application code to release-triggering main merely to save progress.” “Begin.”

**Repository and release evidence.** The current `main` tip at session start is `cb1a35d6b506b2214bb19e56d62e50d8b80e48b6`, with a clean working tree. Fetching the handoff’s planning source `3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21` and diffing confirmed only 11 documentation files / 457 insertions after that source. The four remote branches are `main`, `codex/incognito-voice-guard-20261115`, `codex/reno10-ime-boundary-20261115`, and `jules-board-10408838174702712066`; the open-PR search returned none. Latest signed release remains v1.0.57, 37,949,189 bytes, SHA-256 `a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd`.

**CI and local baseline.** Debug job [133178974688](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906039/job/133178974688) passed APK build, signer verification and upload. `:app:testRunTestsUnitTest` then completed 180 tests with 13 failures: five currency in `ParserTest`, seven URL/autospace in `InputLogicTest`, and one `StringUtilsTest` Emoji assertion. The fail gate failed and debug publication was skipped. Local `./gradlew :app:testRunTestsUnitTest` was attempted with JDK 21.0.13; the wrapper failed before Gradle configuration because it could not download Gradle 8.14.4 (`java.net.SocketException: Network is unreachable`). No local tests ran. `adb` is not installed, so no Reno10 hardware checks were performed.

**B00 status: partial.** Repo/release/CI/concurrency review is complete; device model, ColorOS/API, density/insets and app interaction matrix require the owner’s phone and remain open. No user-device result is claimed. Next exact task is B01: isolate parser fixtures with English-US locale and blank custom currency while retaining explicit custom-₹ behavior. The product `₹` default must not change. Focused and full CI evidence is required before proceeding to B02. See [implementation handoff](ENGINEERING_HANDOFF.md#b00--implementation-baseline-2026-11-16) and [master plan](MASTER_IMPLEMENTATION_PLAN.md).

## 2026-11-16 — B01 parser fixture isolation (in progress)

Starting commit: B00 doc checkpoint `7ab4bc570b18ad79bd5e4d8761ae910f10de2174`. The owner’s B01 requirement is to make the parser/currency fixture deterministic and preserve the production ₹ default. Changed path: `app/src/test/java/helium314/keyboard/KeyboardParserTest.kt`. The fake parser ID now carries a `Locale.US` subtype; setup explicitly sets an empty custom-currency preference for legacy locale-based USD expectations; teardown restores any prior value and clears the locale-info cache; a new `currencyKeyUsesConfiguredRupee` assertion verifies configured ₹ remains honored. No runtime source/default has changed. `git diff --check` passed. Local Gradle execution remains blocked before build configuration because 8.14.4 cannot be downloaded from this environment, so no local test passed or failed. Source changes are on `brrrboard-audit-b01-currency-20261116` so the test workflow can run without exposing unverified app code on `main`; current CI is pending. Exact source commit SHA, focused parser results, full-suite counts, and next step must be added after checking the workflow. Do not start B02 until B01 is green or a precise repair is documented.


## 2026-11-16 — B01 parser fixture result

B01 source commit [e4a4a01](https://github.com/Batmancpu/brrrBoard/commit/e4a4a01ce13edf7020335cb802c4687c165db841) on `brrrboard-audit-b01-currency-20261116`, based on B00 commit `7ab4bc570b18ad79bd5e4d8761ae910f10de2174`, confines the change to parser tests and handoff records. The fixture now uses Locale.US and an explicit blank custom-currency preference; prior preference state and locale cache are restored/cleared around each test; a new assertion verifies configured ₹ remains supported. Product defaults/runtime code are untouched. CI run [43848172243](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243), job [133233579041](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243/job/133233579041), built, signer-verified and uploaded the debug APK. `testRunTests` completed 181 tests. All five currency failures were resolved and the configured-rupee test passed; seven existing URL/autospace failures and the existing Emoji classifier failure remain (8/181). The test gate failed, so debug prerelease publication was skipped. `git diff --check` passed; local Gradle tests remain unrun because Gradle 8.14.4 could not be fetched in this environment. **B01 complete. Next exact task: B02**, isolate URL/autospace fixtures without weakening expected behavior, then rerun focused/full CI. B00 Reno10/device checks are still pending owner hardware access.


## 2026-11-16 — B02 InputLogic fixture triage (in progress)

Starting commit: `2df304cdcd98b7f08bb912f8974fcd93631b24d5` on `brrrboard-audit-b01-currency-20261116`. CI job [133233579041](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243/job/133233579041) reported seven URL/autospace failures; full test task completed 181 tests (five currency failures fixed; one Emoji classifier failure remains). Reading source confirmed defaults in `Defaults.kt` are enabled for URL detection and autospace, while `InputLogicTest.reset()` clears preferences before starting a field. Four failed scenarios expected disabled behavior without setting false. `app/src/test/java/helium314/keyboard/latin/InputLogicTest.kt` now explicitly sets autospace=false for the first phase of `autospace` and `autospaceButWithTextAfter`; URL detection=false for the initial email behavior and disabled-URL whole-word selection. The test expectations and product settings defaults are unchanged. Three failures already configure URL detection=true and have not been edited pending runtime trace after the fixture changes. Current edit is in progress; no CI result yet. `git diff --check` and CI are the next exact actions. Local Gradle remains blocked before configuration (cannot download 8.14.4), and `adb` is unavailable.


**B02 transfer failure and recovery:** commit `5e3c493b3e58ef38916b40d90988dbf4b9af251c` triggered debug run [43850639721](https://github.com/Batmancpu/brrrBoard/actions/runs/43850639721). APK build, signer verification and upload succeeded, but `:app:compileRunTestsUnitTestKotlin` failed before any tests ran. Compiler output showed a parse error at `InputLogicTest.kt:608` and cascading unresolved references. Investigation found the workspace file is intact (1208 lines); the GitHub file response used during tree creation had been truncated by the command-output token ceiling. This is an implementation transfer failure, not a test finding. Recovery: retransmit the complete source/docs in <=5KB chunks, verify exact byte counts and content/blob, then rerun CI. Do not claim B02 behavior is tested until a full CI result arrives.


## 2026-11-16 — B02 fixture CI result and transfer recovery

B02 fixture source was first committed as `5e3c493b3e58ef38916b40d90988dbf4b9af251c`. Its CI run [43850639721](https://github.com/Batmancpu/brrrBoard/actions/runs/43850639721) failed before tests because the remote `InputLogicTest.kt` blob included a literal truncation marker from a token-limited shell response (syntax error at line 608). I repaired this by transferring complete local files in bounded 4 KB chunks and validating byte lengths. Recovery commit [33afb1f4f7e5a7711ca2874090b389d9d4ef2311](https://github.com/Batmancpu/brrrBoard/commit/33afb1f4f7e5a7711ca2874090b389d9d4ef2311) includes the full 1,208-line `InputLogicTest.kt` and context docs; all five changed Git blob hashes match workspace hashes exactly. `git diff --check 2df304c..HEAD` passed.

CI run [43852082788](https://github.com/Batmancpu/brrrBoard/actions/runs/43852082788), job `133246914126`, built the debug APK, signer-verified it and uploaded the artifact. The full suite executed 181 tests. Explicitly setting disabled autospace in two tests and disabled URL detection in two tests fixed four failures without changing expected assertions or product defaults. Three URL-enabled assertions remain failing: `urlDetectionThings` line 305, `stripSeparatorsBeforeAddingToHistoryWithURLDetection` line 323, and `intermediate commit in text field without protocol and with URL detection` line 443; the Emoji classifier test remains the fourth failure. The suite gate failed and prerelease was skipped. Added assertion messages to report actual editor/composing/committed text plus URL setting on those three cases. B02 remains partial. Next exact action: `git diff --check`, commit the diagnostics with updated context, rerun CI, inspect actual states and fix the confirmed InputLogic path. Local Gradle cannot obtain wrapper 8.14.4; no Reno10 device is attached.


**B02 diagnostic result:** CI run [43853548360](https://github.com/Batmancpu/brrrBoard/actions/runs/43853548360) on commit `e934de63bf79274e0efe2c425ff38470ff227337` built/signer-verified/uploaded the APK and ran 181 tests. The three diagnostics are still failing, plus the Emoji test. Although test assertions now include state details, Gradle’s Actions console summary reports only `ComparisonFailure` and source lines; the JUnit XML is not uploaded by the existing workflow. To make a targeted correction from evidence, added an audit build workflow step to upload only `app/build/test-results/testRunTestsUnitTest/*.xml` when the test task fails (7-day artifact retention). This does not affect the release workflow or application code. Next: check diff, checkpoint this workflow update and rerun full CI; inspect downloaded XML expected/actual text before any runtime change.


**B02 runtime failure diagnosed (2026-11-16):** CI [43855310321](https://github.com/Batmancpu/brrrBoard/actions/runs/43855310321) built/signer-verified/uploaded APK and executed 181 tests; three URL cases and one Emoji case failed. New failure-only JUnit XML artifact [14221202444](https://github.com/Batmancpu/brrrBoard/actions/runs/43855310321/artifacts/14221202444) exposed actual values. `urlDetectionThings`: expected `bla.c`, got `bla. c`, with URL detection true. `stripSeparatorsBeforeAddingToHistoryWithURLDetection`: expected `example.com.` composing, but got empty composition, text `example. com.`, and last word `com`. Intermediate URL commit expected last word `bla`, but got `img`; text was `bla. com/img.jpg`, composing only `jpg`, and committed prefix `bla. com/img.`. These failures share a cause: the first URL period leaves PHANTOM space; the URL continuation/restart block required `SpaceState.NONE`, so it did not resume and autospace inserted a real blank. `InputLogic.java` is now changed to allow URL resume with either NONE or PHANTOM, then skip the autospace branch if the composition was restored. This source edit is not yet committed or tested. Next: diff-check, commit with four continuity records, run full CI and inspect URL and Emoji outcomes. Artifact contains synthetic test values only and expires 2026-11-23.


**B02 runtime fix result:** CI [43857301235](https://github.com/Batmancpu/brrrBoard/actions/runs/43857301235) executed 181 tests. The three previously failing URL-enabled checks (`urlDetectionThings`, URL history separator, URL path intermediate commit) now pass after commit `65dbdcff9b2aead72a05eefe2a491ae05e3fa319`. The JUnit report instead showed four implicit-default mismatches: `autospace` expected sentence spacing while URL detection remained on, `autospaceButWithTextAfter` same, `noAutospaceInUrlField` expected non-URL handling in its normal field, and `intermediate commits in text field without protocol` expected URL detection disabled. Each test now writes URL detection=false before starting the relevant editor. Assertions and product defaults remain unchanged. The other failing test is the Emoji classifier, left for dependency batch B03. Next: diff-check, commit four fixtures/context and run full CI to close B02.
