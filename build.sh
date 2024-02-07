#!/bin/bash

# general 

VERSION="1.0"
SERVER_JAR=proximity-"$VERSION".jar
BUILD_DIR_PATH="./build"
BUILD_DIR_PATH=$(realpath $BUILD_DIR_PATH)

# target (override on command line)
# OS=linux ARCH=amd64 ./build.sh

if [ -z "$OS" ]; then
  OS=linux
fi
if [ -z "$ARCH" ]; then
  ARCH=amd64
fi

EXECUTABLE=prxy
if [ $OS = "windows" ]; then
  EXECUTABLE=prxy.exe
fi

BUILD_DIR=$BUILD_DIR_PATH/"$OS-$ARCH"/proximity/
mkdir -p "$BUILD_DIR"

# build server

cd ./proximity.server || exit
mvn package
mv ./target/"$SERVER_JAR" "$BUILD_DIR"/proximity.jar
cp ./src/main/resources/settings.json "$BUILD_DIR"/settings.json
cd ..

# build client

cd ./proximity.client || exit
GOOS=$OS GOARCH=$ARCH go build -o $EXECUTABLE main/main.go
cd ..
mv ./proximity.client/$EXECUTABLE "$BUILD_DIR"

zip "$BUILD_DIR"/proximity-"$OS"-"$ARCH".zip "$BUILD_DIR"/*

