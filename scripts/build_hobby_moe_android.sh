#!/usr/bin/env bash
set -euo pipefail
# Native sparse MoE engine from pinned HobbyLM source; no phone downloads.
HOBBY_SHA="817878f52bf054a4d9aabfdd5360341dd7ce3fa6"
NDK_VER="27.2.12479018"
ndk="$ANDROID_HOME/ndk/$NDK_VER"
if [ ! -d "$ndk" ]; then
  sdkmanager_path=$(find "$ANDROID_HOME/cmdline-tools" -name sdkmanager -type f | head -n1)
  "$sdkmanager_path" "ndk;$NDK_VER"
fi
src="native/moe/vendor/hobbylm"
mkdir -p "$(dirname "$src")"
if [ ! -d "$src/.git" ]; then
  git clone --no-checkout --filter=blob:none https://github.com/harishsg993010/HobbyLM.git "$src"
fi
git -C "$src" fetch --depth 1 origin "$HOBBY_SHA"
git -C "$src" checkout --detach "$HOBBY_SHA"
test "$(git -C "$src" rev-parse HEAD)" = "$HOBBY_SHA"
# Author's README explicitly grants Apache-2.0.
grep -F 'Apache-2.0' "$src/README.md"
cp "$src/README.md" app/src/main/assets/moe/HOBBYLM_SOURCE_README.txt
rustup target add aarch64-linux-android
toolchain="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$toolchain/aarch64-linux-android28-clang"
export CC_aarch64_linux_android="$toolchain/aarch64-linux-android28-clang"
export AR_aarch64_linux_android="$toolchain/llvm-ar"
export RUSTFLAGS="-C target-feature=+neon"
cargo build --manifest-path native/moe/Cargo.toml \
  --target aarch64-linux-android --release
lib="native/moe/target/aarch64-linux-android/release/libvmr_moe.so"
test -s "$lib"
mkdir -p app/src/main/jniLibs/arm64-v8a
cp "$lib" app/src/main/jniLibs/arm64-v8a/libvmr_moe.so
file app/src/main/jniLibs/arm64-v8a/libvmr_moe.so
sha256sum app/src/main/jniLibs/arm64-v8a/libvmr_moe.so
echo "Native routed HobbyLM-1B Sparse MoE linked from $HOBBY_SHA"
