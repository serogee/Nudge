# Nudge 0.4.2

## Added

- group gesture selectors above settings (e1325ae)
- show button actions in three columns (a3e0bfa)
- strengthen maximum gesture haptics (09ce5e3)
- replace timing sliders with bounded inputs (776926f)
- animate visible handles during swipes (07fb8af)
- store brightness popup animation choices (9272a13)
- animate brightness popup transitions (fdcabed)
- strengthen handle badge letter (42c0d99)
- collapse detailed gesture settings (3095386)
- compact color controls and compare slider backgrounds (f7c7a26)
- add a master handle animation switch (72c4bec)

## Fixed

- restore regular panel transitions (92936a9)
- keep handles visible while raising windows (26b76e7)
- retain revealed handles during window handoff (6120680)

## Maintenance

- target the panel search input in its dialog (ef74b7d)
- record handle feedback and control updates (e4ab968)
- explain handle and brightness feedback (4e53441)
- record brightness popup updates (433e330)
- prepare Nudge 0.4.2 (1a883c2)
- record collapsed gesture settings (7c17af5)
- explain handle and color preview controls (d99c5fd)
- record handle and color controls (71fdf8b)

## Compatibility and known limits

- New backups use schema 14. Older backups remain importable. Older Nudge versions cannot import schema-14 backups.
- Existing configurations keep handle animations on. Turn off **Handle animations** to disable pull and reveal feedback for all handles.
- The Android animation setting also controls handle and brightness transitions.
- Color controls use a compact swatch. Tap it to choose a palette color or enter a hex value.
- The brightness preview compares the selected background with the MIUI volume background, #202122.
- Install over 0.4.1 without uninstalling. Recheck saved settings and accessibility after updating.
- Physical Redmi and HyperOS checks, update installation, and battery comparison remain pending.
- The finalizer checks the merged release, builds and signs the APK, verifies its identity, and creates the tag.
