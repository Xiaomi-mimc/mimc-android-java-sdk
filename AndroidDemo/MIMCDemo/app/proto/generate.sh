#!/bin/bash

rm -rf gen

mkdir gen

FILES=`find . -name '*.proto'`
for FILE in $FILES
do
  protoc --javalite_out=./gen $FILE
done

/bin/cp -rf ./gen/com ../src/main/java
rm -rf ./gen

