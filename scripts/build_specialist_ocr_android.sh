#!/usr/bin/env bash
# Build actual multimodal OCR executable for ARM64 with pinned upstream source.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
pin="792fcf4e7501972b595d3aeeb85153f9926f6aef"
ndk="$(find "$ANDROID_HOME/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n1)"
test -f "$ndk/build/cmake/android.toolchain.cmake"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git init -q "$work/llama"
git -C "$work/llama" remote add origin https://github.com/ggml-org/llama.cpp.git
git -C "$work/llama" fetch --depth 1 origin "$pin"
git -C "$work/llama" checkout -q --detach "$pin"
test "$(git -C "$work/llama" rev-parse HEAD)" = "$pin"
mkdir -p "$root/app/src/main/assets/licenses"
cp "$work/llama/LICENSE" "$root/app/src/main/assets/licenses/llama.cpp-MIT.txt"
test -s "$root/app/src/main/assets/licenses/llama.cpp-MIT.txt"
cmake -S "$work/llama" -B "$work/build" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_static \
  -DBUILD_SHARED_LIBS=OFF -DGGML_OPENMP=OFF -DGGML_BLAS=OFF -DGGML_NATIVE=OFF \
  -DGGML_VULKAN=OFF -DGGML_OPENCL=OFF -DLLAMA_CURL=OFF -DLLAMA_BUILD_TESTS=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF \
  -DLLAMA_BUILD_TOOLS=ON -DLLAMA_BUILD_MTMD=ON -DMTMD_VIDEO=OFF
cmake --build "$work/build" --target llama-mtmd-cli --parallel 4
exe="$work/build/bin/llama-mtmd-cli"
test -s "$exe"
mkdir -p "$root/app/src/main/jniLibs/arm64-v8a"
cp "$exe" "$root/app/src/main/jniLibs/arm64-v8a/libvmr_mtmd_cli.so"
readelf -h "$exe" | grep -q 'DYN (Position-Independent Executable file)'
echo "VMR-019: ARM64 local multimodal OCR executable compiled"
