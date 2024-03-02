# Simple wrapper to generate different sizes of product_tokens.csv

./productokens.exe 100000
Copy-Item "./product_tokens.csv" -Destination "./product_tokens-100K.csv"
./productokens.exe 500000
Copy-Item "./product_tokens.csv" -Destination "./product_tokens-500K.csv"


