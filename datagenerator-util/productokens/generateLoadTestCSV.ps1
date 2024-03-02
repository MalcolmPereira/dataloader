# Description: This script is used to generate the load test CSV files for the productokens.exe
# This takes two parameters
# 1. runvalue: Number of CSV files to be generated
# 2. data_path: The path where the CSV files will be generated
# We want the csv files to be genrated in the same directory as the JMeter script
# Usage ./generateLoadTestCSV.sh 10 ../../dataloader-jmeter/
# The above command will generate 10 csv files in the ../../dataloader-jmeter/LOAD_TEST directory


param(
	[int]$runvalue = 5,
	[string]$data_path = $PSScriptRoot
)

Remove-Item -Path  $data_path"LOAD_TEST" -Recurse -ErrorAction SilentlyContinue

New-Item -Path $data_path -Name  "LOAD_TEST" -ItemType "Directory" 

$data = @()

$count = 0
while ($count -lt $runvalue) {
    ./productokens.exe 25000 
	Copy-Item "./product_tokens.csv" -Destination $data_path"LOAD_TEST/product_tokens-$count.csv"
    $data += New-Object PSObject -Property @{File = "LOAD_TEST/product_tokens-$count.csv"}
	$count++
}

# Export the data to a CSV file
$data | Export-Csv -Path $data_path"LOAD_TEST/load-test.csv" -NoTypeInformation