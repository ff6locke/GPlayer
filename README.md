# GPlayer

An original Android TV media player project for Rocktek GX1, built from scratch. It aims for an MX Player-class feature set without using MX Player's proprietary source code or codec binaries.

## Initial build

The first milestone is a working Android application shell with local-file playback using LibVLC and an Android TV-friendly interface. Further milestones will add track selection, subtitles, hardware/software decoder controls, network sources, playback gestures, resume/history, and audio passthrough diagnostics.

## Build

GitHub Actions builds a debug APK on pushes to `main` and manual workflow dispatch. Download the `GPlayer-debug-apk` artifact from the workflow run.

## Important codec note

Actual Dolby Atmos, TrueHD, DTS-HD, and multichannel passthrough depend on the Rocktek firmware, Android audio APIs, HDMI output mode, receiver capabilities, and the media stream. The app cannot guarantee every codec or passthrough mode on every device.
