#!/bin/bash

echo input file: $1
echo output file: $2

python Preprocessing.py $1 tmp.csv
sort -n tmp.csv > tmp_sorted.csv
sed s/,/'\n'/g tmp_sorted.csv > $2

rm tmp.csv tmp_sorted.csv