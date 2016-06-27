#!/bin/bash

mvn clean install -Dmirage2.on=on

scp pkg/target/dspace-pkg-5.4.deb root@dspacepkg-on-1:/tmp
