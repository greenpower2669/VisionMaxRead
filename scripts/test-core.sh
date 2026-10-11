#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/contracts
javac -encoding UTF-8 -d core/build/contracts core/src/main/java/fr/visionmaxread/core/*.java core/src/test/java/fr/visionmaxread/core/*.java
java -cp core/build/contracts fr.visionmaxread.core.Contracts
