#!/bin/bash
function runImport() {
    if [ -d "$i" ]; then
        echo $i
        mapfile=`echo "mapfile${i##*$DATADIR/}" | tr '/' '-'`
        touch $MAPFILEDIR/$mapfile
        ${DSPACEDIR}/bin/dspace import -a -R -s "$i" -m "$MAPFILEDIR/$mapfile" -e "$ADMIN" -l -q -c "$TARGETCOLLECTION"
    fi
}
