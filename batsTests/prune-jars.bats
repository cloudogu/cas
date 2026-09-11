#!/usr/bin/env bats

setup() {
  lib_dir="$(mktemp -d)"
}

teardown() {
  rm -rf "$lib_dir"
}

@test "keeps the newest Jackson 2 and Jackson 3 jars separately" {
  touch \
    "$lib_dir/jackson-annotations-2.21.jar" \
    "$lib_dir/jackson-annotations-2.22.jar" \
    "$lib_dir/jackson-databind-2.21.5.jar" \
    "$lib_dir/jackson-databind-2.22.1.jar" \
    "$lib_dir/jackson-databind-3.1.5.jar" \
    "$lib_dir/jackson-databind-3.2.1.jar"

  run sh /workspace/resources/prune-jars.sh "$lib_dir"

  [ "$status" -eq 0 ]
  [ ! -e "$lib_dir/jackson-annotations-2.21.jar" ]
  [ -e "$lib_dir/jackson-annotations-2.22.jar" ]
  [ ! -e "$lib_dir/jackson-databind-2.21.5.jar" ]
  [ -e "$lib_dir/jackson-databind-2.22.1.jar" ]
  [ ! -e "$lib_dir/jackson-databind-3.1.5.jar" ]
  [ -e "$lib_dir/jackson-databind-3.2.1.jar" ]
}

@test "keeps Jackson major versions separate for jars with suffixes" {
  touch \
    "$lib_dir/jackson-core-2.20.0-rc1.jar" \
    "$lib_dir/jackson-core-2.21.0-rc1.jar" \
    "$lib_dir/jackson-core-3.0.0-rc1.jar" \
    "$lib_dir/jackson-core-3.1.0-rc1.jar"

  run sh /workspace/resources/prune-jars.sh "$lib_dir"

  [ "$status" -eq 0 ]
  [ ! -e "$lib_dir/jackson-core-2.20.0-rc1.jar" ]
  [ -e "$lib_dir/jackson-core-2.21.0-rc1.jar" ]
  [ ! -e "$lib_dir/jackson-core-3.0.0-rc1.jar" ]
  [ -e "$lib_dir/jackson-core-3.1.0-rc1.jar" ]
}

@test "continues to prune ordinary duplicate jars" {
  touch \
    "$lib_dir/example-1.9.0.jar" \
    "$lib_dir/example-1.10.0.jar" \
    "$lib_dir/example-2.0.0.jar"

  run sh /workspace/resources/prune-jars.sh "$lib_dir"

  [ "$status" -eq 0 ]
  [ ! -e "$lib_dir/example-1.9.0.jar" ]
  [ ! -e "$lib_dir/example-1.10.0.jar" ]
  [ -e "$lib_dir/example-2.0.0.jar" ]
}
