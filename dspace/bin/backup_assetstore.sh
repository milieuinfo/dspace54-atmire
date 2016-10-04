#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/backup_script.sh
getproperty(){
    grep "^assetstore" "$DSPACEDIR/config/dspace.cfg" | cut -d'=' -f2
}

PATH_TO_BACKUP=$(getproperty)

echo "Starting backup of asset store directory $@ on `date +'%Y-%m-%d %H:%M:%S.%3N'`"
backup_dir $@
echo "Backup of asset store directory $@ completed on `date +'%Y-%m-%d %H:%M:%S.%3N'`"
