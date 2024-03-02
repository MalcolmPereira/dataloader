#! /usr/bin/env sh

# Description: This script is used to generate the load test CSV files for the productokens.exe
# This takes two parameters
# 1. runvalue: Number of CSV files to be generated
# 2. data_path: The path where the CSV files will be generated
# We want the csv files to be genrated in the same directory as the JMeter script
# Usage ./generateLoadTestCSV.sh 10 ../../dataloader-jmeter/
# The above command will generate 10 csv files in the ../../dataloader-jmeter/LOAD_TEST directory

echo "Generating Load Test CSV file"

#check if run value argument is passed else assign it a value 5
runvalue=5
if [ ! -z "$1" ]; then
  runvalue=$1
fi

#check if data path value argument is passed else assign it default current execution path
datapath=$(pwd)
if [ ! -z "$2" ]; then
  datapath=$2
fi

echo "runvalue: $runvalue"
echo "datapath: $datapath"

#check if a directory named "LOAD_TEST" exists in $datapath in which case delete recursively
if [ -d "$datapath/LOAD_TEST" ]; then
  rm -rf $datapath/LOAD_TEST
fi
mkdir $datapath/LOAD_TEST

#loop through the runvalue and generate csv files
for i in $(seq 1 $runvalue)	
do
  ./productokens 1000 
  mv "./product_tokens.csv" $datapath/"/LOAD_TEST/product_tokens-$i.csv"
  echo $data_path"LOAD_TEST/product_tokens-$i.csv" >> $datapath"/LOAD_TEST/load-test.csv"
done
