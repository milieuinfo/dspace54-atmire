#!/usr/bin/env bash

restore_dir(){
  if [ -z $1 ] ; then
    echo "no target directory provided"
  else
    TARGETDIR=$1
    if [ -f $TARGETDIR ] ; then
      echo "target directory is a file"
    else
      if [ -d $TARGETDIR ] ; then
        rm -rf $PATH_TO_RESTORE
        RC=1 
        while [[ $RC -ne 0 ]]
        do
          rsync -av $TARGETDIR/* $PATH_TO_RESTORE
          RC=$?
        done
      else
        echo "target directory does not exist"
      fi
    fi
  fi
}
