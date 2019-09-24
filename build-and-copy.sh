#!/bin/bash

mvn clean install -Dmirage2.on=on

scp pkg/target/dspace-pkg-5.4.0.deb root@10.30.32.20:/tmp
