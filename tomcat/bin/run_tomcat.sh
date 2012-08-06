#!/bin/sh

BIN_DIR=$(dirname $0)

CATALINA_OPTS="-Dhttp.port=$PORT" $BIN_DIR/catalina.sh run
