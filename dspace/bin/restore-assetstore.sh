#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/backup_script.sh
getproperty(){
    grep "^assetstore" "$DSPACEDIR/config/dspace.cfg" | cut -d'=' -f2
}

PATH_TO_RESTORE=$(getproperty)
#echo $PATH_TO_RESTORE
restore_dir $@
