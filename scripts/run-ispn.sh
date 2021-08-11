#!/bin/bash

ISPN_VERSION=11.0.3.Final
if [ ! -f "infinispan-server-$ISPN_VERSION.zip" ]
then
	wget https://downloads.jboss.org/infinispan/$ISPN_VERSION/infinispan-server-$ISPN_VERSION.zip
fi
rm -r infinispan-server-$ISPN_VERSION && \
	unzip infinispan-server-$ISPN_VERSION.zip && \
	cd infinispan-server-$ISPN_VERSION && \
       	bin/cli.sh user create user -p "1234" && \
       	cd .. && \
       	infinispan-server-$ISPN_VERSION/bin/server.sh
