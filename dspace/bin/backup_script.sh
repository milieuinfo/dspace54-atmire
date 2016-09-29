#!/usr/bin/env bash

rsync_loop(){
    echo "rsync -av $1/* $2"
    rsync_output="start"
    set -e
    until [[ "$rsync_output" == "done" ]]
    do
       results_rsync=$(rsync -av $1/* $2)
       #echo $results_rsync
       TEST=`echo "$results_rsync" | grep -o "data" | sed q`
       if [[ "${TEST}" != "data" ]]
       then
            rsync_output="done"
       fi
    done
}

backup_dir(){
  if [ -z $1 ] ; then
    echo "no target directory provided"
  else
    TARGETDIR=$1
    if [ -f $TARGETDIR ] ; then
      echo "target directory is a file"
    else
      mkdir -p $TARGETDIR
      rsync_loop $PATH_TO_BACKUP $TARGETDIR
    fi
  fi
}

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
        rsync_loop $TARGETDIR $PATH_TO_RESTORE
      else
        echo "target directory does not exist"
      fi
    fi
  fi
}