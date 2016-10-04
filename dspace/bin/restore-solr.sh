#!/usr/bin/env bash

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

source $DSPACEDIR/bin/backup_script.sh

PATH_TO_RESTORE=$DSPACEDIR/solr
restore_dir $@
