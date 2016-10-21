#!/bin/bash

echo $DSPACEDIR
source $DSPACEDIR/bin/import/import-function.sh

for i in $DATADIR/{1..19}/*; do
    runImport
done

for i in $DATADIR/{80..89}/*; do
    runImport
done

for i in $DATADIR/{90..99}/*; do
    runImport
done
