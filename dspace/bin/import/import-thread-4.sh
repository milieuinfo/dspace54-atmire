#!/bin/bash

echo $DSPACEDIR
source $DSPACEDIR/bin/import/import-function.sh

for i in $DATADIR/{40..49}/*; do
    runImport
done

for i in $DATADIR/{50..59}/*; do
    runImport
done
