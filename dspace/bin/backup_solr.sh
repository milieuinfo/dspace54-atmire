#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/backup_script.sh

PATH_TO_BACKUP=$DSPACEDIR/solr
backup_dir $@
