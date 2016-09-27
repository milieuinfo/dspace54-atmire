#!/usr/bin/env bash

backup_dir(){
  if [ -z $1 ] ; then
    echo "no target directory provided"
  else
    TARGETDIR=$1
    if [ -f $TARGETDIR ] ; then
      echo "target directory is a file"
    else
      mkdir -p $TARGETDIR
      RC=1 
      while [[ $RC -ne 0 ]]
      do
        rsync -av $PATH_TO_BACKUP/* $TARGETDIR
        RC=$?
      done
    fi
  fi
}
