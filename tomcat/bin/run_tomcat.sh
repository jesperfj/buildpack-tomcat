#!/bin/sh

BIN_DIR=$(dirname $0)

CATALINA_OPTS="-Dhttp.port=$PORT"

if [ ! -z "$DATABASE_URL" ] ; then

	JDBC_HOST_PORT=$(echo $DATABASE_URL | sed "s/[^@]*@\(.*\)*/\1/")
	JDBC_USERPASS=$(echo $DATABASE_URL | sed "s/^.*:\/\/\([^@]*\)@.*$/\1/")
	JDBC_USER=$(echo $JDBC_USERPASS | cut -d : -f 1)
	JDBC_PASSWORD=$(echo $JDBC_USERPASS | cut -d : -f 2)
	JDBC_URL="jdbc:postgresql://$JDBC_HOST_PORT"

	echo "Detected DATABASE_URL. Configuring Java System properties: jdbc.url=$JDBC_URL, jdbc.user=$JDBC_USER, jdbc.password=********"

	CATALINA_OPTS="$CATALINA_OPTS -Djdbc.url=$JDBC_URL -Djdbc.user=$JDBC_USER -Djdbc.password=$JDBC_PASSWORD"
fi
export CATALINA_OPTS

$BIN_DIR/catalina.sh run
