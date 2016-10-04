#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/backup_script.sh

PATH_TO_BACKUP=$DSPACEDIR/solr

echo "Starting backup of SOLR directory $@ on `date +'%Y-%m-%d %H:%M:%S.%3N'`"
backup_dir $@
echo "Backup of SOLR directory $@ completed on `date +'%Y-%m-%d %H:%M:%S.%3N'`"
