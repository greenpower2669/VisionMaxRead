#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
archive_jar="${XZ_JAR:-}"
if [[ -z "$archive_jar" ]]; then
  archive_jar=$(python3 - <<'PY'
import pathlib,os
root=pathlib.Path(os.environ.get('GRADLE_USER_HOME',str(pathlib.Path.home()/'.gradle')))
files=list((root/'caches/modules-2/files-2.1/org.tukaani/xz/1.10').glob('**/xz-1.10.jar'))
if not files:raise SystemExit('Run the Android build first, or set XZ_JAR to the org.tukaani:xz:1.10 jar')
print(files[0])
PY
)
fi
mkdir -p app/build/archive-test
javac -encoding UTF-8 -cp "$archive_jar" -d app/build/archive-test app/src/main/java/fr/visionmaxread/ModelArchive.java scripts/PackedModelContract.java
java -cp "app/build/archive-test:$archive_jar" fr.visionmaxread.PackedModelContract scripts/fixtures/archive-test.xz.base64 32768 e11360251d1173650cdcd20f111d8f1ca2e412f572e8b36a4dc067121c1799b8
