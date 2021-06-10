#!/bin/bash

echo input file: $1
echo output file: $2

echo copy from hadoop...
hadoop fs -copyToLocal /user/noervaag/GeoTweets/$1

echo convert to csv...
java -jar GeoTwitterFilter-1.0-SNAPSHOT.jar $1 tmp_1.txt.gz

echo preprocess...
python3 Preprocessing.py tmp_1.txt.gz tmp_2.csv.gz

echo sort and split...
zcat tmp_2.csv.gz | sort -n | sed s/'\t'/'\n'/g | gzip > $2

rm tmp_1.txt.gz tmp_2.csv.gz

