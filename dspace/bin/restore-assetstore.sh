#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/restore_script.sh
getproperty(){
    grep "^assetstore" "$DSPACEDIR/config/dspace.cfg" | cut -d'=' -f2
}

PATH_TO_BACKUP=$(getproperty)
#echo $PATH_TO_BACKUP
restore_dir $@
