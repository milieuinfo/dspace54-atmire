#!/usr/bin/env bash

rsync_loop(){
    rsync_output="start"
    set -e
    until [[ "$rsync_output" == "done" ]]
    do
       echo "Copying files from $1 to $2, please wait..."
       results_rsync=$(rsync -av --bwlimit=10000 --perms --delete --delete-excluded --exclude '*~' $1/ $2)
       #echo "Result: $results_rsync"
       TEST=`echo "$results_rsync" | grep -v "bytes/sec" | grep -o "/" | sed q`
       #echo "Test: $TEST"
       if [[ -z $TEST ]]
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