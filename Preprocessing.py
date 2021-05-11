#!/usr/bin/env python
# coding: utf-8
import pandas as pd
import numpy as np
import reverse_geocoder as rg
import preprocessor as p
from gensim.parsing.preprocessing import remove_stopwords
from multiprocessing import Pool
import sys
import csv

in_path = sys.argv[1]
out_path = sys.argv[2]


# Function to parallelize operations
def parallelize_dataframe(df, func, n_cores=4):
    df_split = np.array_split(df, n_cores)
    pool = Pool(n_cores)
    df = pd.concat(pool.map(func, df_split))
    pool.close()
    pool.join()
    return df


def clean_f(df):
    try:
        df['text'] = df['text'].apply(p.clean)
    except:
        print('clean failed on texts: ', df['text'])
    return df


def remove_stopwords_f(df):
    df['text'] = df['text'].apply(remove_stopwords)
    return df


remove_short_words = lambda x: ' '.join([item for item in x.split(" ") if len(item) > 2])


def remove_short_words_f(df):
    df['text'] = df['text'].apply(remove_short_words)
    return df


def remove_numbers_f(df):
    df['text'] = df['text'].str.lower() \
        .str.replace('\d+', '').str.replace('a{2,}', 'a').str.replace('b{3,}', 'b').str.replace('c{3,}', 'c') \
        .str.replace('d{3,}', 'd').str.replace('e{3,}', 'e').str.replace('f{3,}', 'f').str.replace('g{3,}', 'g') \
        .str.replace('h{3,}', 'h').str.replace('i{2,}', 'i').str.replace('j{3,}', 'j').str.replace('k{3,}', 'k') \
        .str.replace('l{3,}', 'l').str.replace('m{3,}', 'm').str.replace('n{3,}', 'n').str.replace('o{3,}', 'o') \
        .str.replace('p{3,}', 'p').str.replace('q{3,}', 'q').str.replace('r{3,}', 'r').str.replace('s{3,}', 's') \
        .str.replace('t{3,}', 't').str.replace('u{2,}', 'u').str.replace('v{3,}', 'v').str.replace('w{3,}', 'w') \
        .str.replace('x{3,}', 'x').str.replace('y{2,}', 'y').str.replace('z{3,}', 'z').str.replace('_', ' ') \
        .str.replace(' rt ', '').str.replace('#', '').str.replace('[^\w\s]', ' ').str.replace('\s\s+', ' ')
    return df


# Preprocessing
# Remove rows with missing text, filter out non-english tweets. Get detailed locations and timestamp and tokenize the text.
# * Fetch more location info from longitude and latitude using reverse_encoder https://github.com/thampiman/reverse-geocoder
# * Convert ms timestamps to datetime object.
# * Preprocess text using the preprocessor https://github.com/s/preprocessor and remove stopwords using gensim.
def preprocess(df):
    df = df.drop(df[df.lang != 'en'].index)
    df = df.drop(['lang'], axis=1)
    df = df.drop(df[df.text == ''].index)
    df = df.drop(df[df.text.isna()].index)
    df = df.reset_index()
    df = df.drop(['index'], axis=1)

    # Get location information
    coordinates = list(df[['latitude', 'longitude']].itertuples(index=False, name=None))
    locations = rg.search(coordinates)
    locations_df = pd.json_normalize(locations)[['name', 'admin1', 'admin2', 'cc']]
    df = pd.concat([df, locations_df], axis=1)

    # Clean
    p.set_options(p.OPT.URL, p.OPT.MENTION, p.OPT.RESERVED, p.OPT.EMOJI, p.OPT.SMILEY, p.OPT.NUMBER)
    df = parallelize_dataframe(df, clean_f)

    # Make lower case and remove numbers and white space, must be done after cleaning
    df = parallelize_dataframe(df, remove_numbers_f)
    df = parallelize_dataframe(df, remove_numbers_f)

    # Remove stopwords, must be done after cleaning and removal of white space
    df = parallelize_dataframe(df, remove_stopwords_f)
    # Remove words with less than 3 characters, should be done after removing stop words
    df = parallelize_dataframe(df, remove_short_words_f)

    df = df.drop(df[df.text == ''].index)
    df = df.drop(['admin2'], axis=1)

    return df


chunks = pd.read_csv(in_path, encoding='cp1252', sep="\t",
                     usecols=['timestamp_ms', 'longitude', 'latitude', 'text', 'lang'], chunksize=10 ** 6)

fmt = '%d\n%.8f\n%.8f\n%s\n%s\n%s\n%s'
i = 1
with open(out_path, "ab") as f:
    for chunk in chunks:
        print("Chunk ", i)
        df = preprocess(chunk)
        # np.savetxt(f, df.values, fmt=fmt, delimiter='\r\n')
        df.to_csv(out_path, header=False, mode="a", index=False, quoting=csv.QUOTE_NONE, quotechar="", escapechar="\\")
        i += 1
