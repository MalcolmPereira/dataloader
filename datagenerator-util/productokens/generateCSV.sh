#! /usr/bin/env sh

# Simple wrapper to generate different sizes of product_tokens.csv


./productokens 100000
mv product_tokens.csv product_tokens-100K.csv   
./productokens 500000
mv product_tokens.csv product_tokens-500K.csv   

