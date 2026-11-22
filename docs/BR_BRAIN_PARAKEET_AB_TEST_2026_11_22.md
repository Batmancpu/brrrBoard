# Archived: Ultra model comparison removed

On 22 November 2026, the owner selected the installed Parakeet Ultra model for
keyboard dictation and cancelled the side-by-side A/B workflow. This archive
retains the measured device result: on one 4.736-second English take, Ultra
reported 12% WER versus 25% for 110M, with 4,152 ms load / 683 ms decode /
775,982 KB peak PSS versus 1,366 ms / 223 ms / 450,352 KB for 110M. These
figures describe that single take and device only.

The settings comparison screen, isolated test service, comparison engine,
production-signed test APK job, split-download path, and compare instructions
are removed. Normal updates do not include model weights, so the verified Ultra
model already stored in private app data remains the keyboard's active model
across the update. The pinned source, license, lock file and explicit,
off-by-default future bundling option remain in the repository for a later
request.

The latest source status is recorded in
[IMPLEMENTATION_STATUS_2026_11_22.md](IMPLEMENTATION_STATUS_2026_11_22.md).
