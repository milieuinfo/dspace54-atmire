#!/bin/bash

echo $DSPACEDIR
source $DSPACEDIR/bin/import/import-function.sh

for i in $DATADIR/{30..39}/*; do
    runImport
done

for i in $DATADIR/{60..69}/*; do
    runImport
done
