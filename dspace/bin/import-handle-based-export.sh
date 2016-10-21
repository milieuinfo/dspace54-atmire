#!/bin/bash

if [ -z $DATADIR ]; then
  echo "You need to provide the directory that contains the export as variable DATADIR"
  exit
fi

if [ -z $MAPFILEDIR ]; then
  echo "You need to provide the directory to store mapfiles as variable MAPFILEDIR"
  exit
fi

if [ -z $ADMIN ]; then
  echo "You need to provide an admin eperson account as variable ADMIN"
  exit
fi

if [ -z $TARGETCOLLECTION ]; then
  echo "You need to provide the target collection handle as variable TARGETCOLLECTION"
  exit
fi

BINDIR=`dirname $0`
DSPACEDIR=`cd "$BINDIR/.." ; pwd`

export DSPACEDIR
export DATADIR
export MAPFILEDIR
export ADMIN
export TARGETCOLLECTION

${DSPACEDIR}/bin/import/import-thread-1.sh >> ${DSPACEDIR}/log/import-log-thread-1.log &
${DSPACEDIR}/bin/import/import-thread-2.sh >> ${DSPACEDIR}/log/import-log-thread-2.log &
${DSPACEDIR}/bin/import/import-thread-3.sh >> ${DSPACEDIR}/log/import-log-thread-3.log &
${DSPACEDIR}/bin/import/import-thread-4.sh >> ${DSPACEDIR}/log/import-log-thread-4.log &

echo "The import threads have been started asynchronously. Please check the logs for progess."
echo "Once the import is complete, make sure to run \"bin/dspace index-discovery\"!"
