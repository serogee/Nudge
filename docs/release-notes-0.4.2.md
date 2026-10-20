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

## Fixed

- restore regular panel transitions (92936a9)
- keep handles visible while raising windows (26b76e7)
- retain revealed handles during window handoff (6120680)

## Maintenance

- target the panel search input in its dialog (ef74b7d)
- record handle feedback and control updates (e4ab968)
- explain handle and brightness feedback (4e53441)
- record brightness popup updates (433e330)

## Compatibility and known limits

- Install over 0.4.1 without uninstalling to retain settings. Recheck accessibility after updating.
- New backups use schema 13. Older backups remain importable. Older Nudge versions cannot import schema-13 backups.
- Handle and brightness transitions follow the Android animation setting.
- Physical Redmi and HyperOS checks, update installation, and battery comparison remain pending.
- The finalizer reruns release checks, signs the APK, verifies its identity, and creates the tag after the GitHub merge.
