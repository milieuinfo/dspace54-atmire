#!/bin/bash

echo $DSPACEDIR
source $DSPACEDIR/bin/import/import-function.sh

for i in $DATADIR/{20..29}/*; do
    runImport
done

for i in $DATADIR/{70..79}/*; do
    runImport
done