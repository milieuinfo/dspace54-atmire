#!/bin/bash

mvn install

scp target/dspace-pkg-5.4.deb root@dspacepkg-on-1:/tmp
