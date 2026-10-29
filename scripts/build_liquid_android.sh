#!/usr/bin/env bash
set -euo pipefail
# GitHub Actions only. Never downloads source or weights in the Android app.
LLAMA_SHA=36a0e2ffc0b352b774f8d71c43668b75b3061ef5
NDK_VERSION=27.2.12479018
: "${ANDROID_HOME:?ANDROID_HOME missing}"
ndk="$ANDROID_HOME/ndk/$NDK_VERSION"
if [ ! -d "$ndk" ]; then
  sdkmanager_path=$(find "$ANDROID_HOME/cmdline-tools" -name sdkmanager -type f | head -n1)
  "$sdkmanager_path" "ndk;$NDK_VERSION"
fi
mkdir -p "$RUNNER_TEMP/vmr-llama"
src="$RUNNER_TEMP/vmr-llama/source"
if [ ! -d "$src/.git" ]; then
  git clone --no-checkout --filter=blob:none https://github.com/ggml-org/llama.cpp.git "$src"
fi
git -C "$src" fetch --depth 1 origin "$LLAMA_SHA"
git -C "$src" checkout --detach "$LLAMA_SHA"
test "$(git -C "$src" rev-parse HEAD)" = "$LLAMA_SHA"
# Statically linked llama.cpp is MIT licensed. Bundle its exact pinned license.
cp "$src/LICENSE" app/src/main/assets/liquid/LLAMA_CPP_LICENSE
test -s app/src/main/assets/liquid/LLAMA_CPP_LICENSE
cmake -S native/liquid -B "$RUNNER_TEMP/vmr-llama/build" \
  -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DCMAKE_BUILD_TYPE=Release \
  -DLLAMA_SOURCE="$src"
cmake --build "$RUNNER_TEMP/vmr-llama/build" --config Release \
  --target vmr_liquid --parallel 3
mkdir -p app/src/main/jniLibs/arm64-v8a
lib=$(find "$RUNNER_TEMP/vmr-llama/build" -name 'libvmr_liquid.so' -type f | head -n1)
test -s "$lib"
cp "$lib" app/src/main/jniLibs/arm64-v8a/libvmr_liquid.so
file app/src/main/jniLibs/arm64-v8a/libvmr_liquid.so
sha256sum app/src/main/jniLibs/arm64-v8a/libvmr_liquid.so
echo "VMR-012 JNI library from pinned llama.cpp $LLAMA_SHA"
