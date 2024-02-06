#!/bin/sh
#
VERSION="1.0"
SERVER_JAR=proximity-"$VERSION".jar

BUILD_DIR="./build"
BUILD_DIR=$(realpath $BUILD_DIR)
mkdir -p "$BUILD_DIR"

# build client

cd ./proximity.client
go build -o prxy main/main.go
cd ..
mv ./proximity.client/prxy "$BUILD_DIR"

# build server

cd ./proximity.server
mvn package
mv ./target/"$SERVER_JAR" "$BUILD_DIR"/proximity.jar
