#!/usr/bin/env bash
# build.sh — compile the Java sources without Maven.
#
# Maven is the canonical build (see pom.xml) but requires internet
# access to Maven Central the first time it runs. This script uses
# plain javac so the project can be built and run in air-gapped
# environments (e.g. classroom labs).
#
# Usage:
#     ./build.sh           # compile sources into ./out
#     ./build.sh clean     # remove ./out
#
set -euo pipefail

if [[ "${1:-}" == "clean" ]]; then
    rm -rf out
    echo "Cleaned build directory."
    exit 0
fi

mkdir -p out
SOURCES=$(find src/main/java -name "*.java")
javac -d out --source 17 --target 17 $SOURCES
echo "Compiled $(echo "$SOURCES" | wc -l) source files into out/."
echo
echo "Run:"
echo "  java -cp out com.aiu.cse112.Main                                 # demo"
echo "  java -ea -cp out com.aiu.cse112.CorrectnessRunner                # tests"
echo "  java -cp out com.aiu.cse112.benchmark.PerformanceBenchmark       # benchmarks"
