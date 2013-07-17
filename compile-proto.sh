#!/bin/bash

PROTOPATH=$(cd "$(dirname "$0")/src/main/protobuf"; pwd)
PROTOFILES=`find $PROTOPATH -iname *.proto | xargs $1`

rm -rf $PROTOPATH/../java
mkdir $PROTOPATH/../java
protoc -I=$PROTOPATH --java_out=$PROTOPATH/../java/ $PROTOFILES
echo "ok"
