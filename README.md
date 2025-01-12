Very simple Android app to provide an STT engine service and an IME voice input service using [whisper.cpp](https://github.com/ggerganov/whisper.cpp/) back end.

Includes ggml-tiny.en.bin - a whisper tiny model for English language only. 

To build;

check out the repo (git clone --depth=1 https://github.com/gavine99/whispercppstt.git)

change to project dir (cd whispercppstt)

check out the whisper.cpp submodule (git submodule update --init --recursive)

load project in android studio

build
