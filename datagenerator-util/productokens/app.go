/*
Simple Utility to generate CSV file containing product tokens

CSV file will be used to import product tokens into the database and contain following columns:

	code,region,serial,name,values

Where

	code is the product code, this uniquely identifies the product

	region is the region code, this uniquely identifies the region the product is in

	serial is the product serial number, this uniquely identifies the product serial in the region

	name is the token name, this is the global product token

	values is the token value, this is a large string that contains the token values that can be used to describe and group this product
	                           there may be instances where for the same code, region and serial there could be multiple grouping of token values
							   so same product in different region codes will have different token values

Example:
1,US,1,Product_Token_1,"{""key1"":""value1"",""key2"":""value2""}....etc these can be very large strings"
2,US,2,Product_Token_2,"{""key1"":""value1"",""key2"":""value2""}....etc these can be very large strings"
3,US,3,Product_Token_3,"{""key1"":""value1"",""key2"":""value2""}....etc these can be very large strings"
....

#

# The binary is available as productokens and productokens.exe for windows

To run the utility use the following command:
./productokens 1000
or
./productokens.exe 1000

which will generate a CSV file with 1000 records
*/
package main

import (
	"encoding/csv"
	"fmt"
	"log"
	"os"
	"strconv"
	"strings"
	"time"
)

// create a string array to hold the product tokens, we then randomly select a token from the array to generate the product tokens
var REGIONS = []string{"USA", "JPN", "DEU", "IND", "GBR", "FRA"}
var REGIONS_PRELOAD = []string{"ESP", "MEX", "TUR", "NLD", "SAU", "CHE", "ARG", "TWN", "SWE", "POL", "BEL", "CHN"}
var LARGE_TOKEN_STRING = `"key1": "Lorem ipsum dolor sit amet, consectetur adipiscing elit.","key2": "Maecenas vel magna et neque sodales interdum. Ut ve","key3": "l erat bibendum, hendrerit odio sit amet, posuere en","key4": "im. Sed dignissim, orci non scelerisque fringilla, m","key5": "auris augue vehicula dolor, id pharetra lectus nequ","key6": "e id mi. Nullam non fermentum odio. Vivamus eget vo","key7": "lutpat leo. Ut nec vestibulum arcu, id dignissim ur","key8": "na. Integer tincidunt id magna vitae vehicula. Maur","key9": "is egestas augue eu turpis egestas, eu tincidunt du","key10": "i finibus. Curabitur sit amet nisl non sapien comm","key11": "odo rutrum a at mauris. Pellentesque eget diam vel","key12": " ligula feugiat tincidunt et at arcu. Fusce ut tin","key13": "cidunt magna, at pharetra purus. Cras varius mi ni","key14": "si, eget ultricies nisi elementum nec. Donec vel bi","key15": "bendum quam. Proin ut diam ut erat volutpat auctor ","key16": "nec a tellus. Aenean id tellus et magna porttitor e","key17": "get ac a diam. Morbi placerat velit in nulla fringu","key18": "lla, eu consectetur nulla cursus. Nunc nec ipsum ac","key19": " dui blandit eleifend non vitae risus. Integer temp","key20": "us commodo lectus, a efficitur ipsum eleifend at. M","key21": "aecenas ullamcorper, lectus non sagittis ullamcorpe","key22": "r, ante libero sodales magna, non vehicula ipsum er","key23": "at eget justo. Aliquam quis justo eu urna tincidun","key24": "t commodo. Duis eu sapien vitae nisl commodo biben","key25": "dum. Nulla ut lectus lectus. Integer tincidunt leo","key26": " eget felis interdum, vel volutpat libero iaculis. ","key27": "Curabitur ultrices est sed semper iaculis. Nulla ut","key28": " felis et enim facilisis posuere. Nullam viverra tr","key29": "istique lacus, sit amet consectetur dolor. In et n","key30": "unc vitae nulla vehicula accumsan id at magna. Pro","key31": "in at aliquam est. In feugiat, neque ut ullamcorper","key32": " volutpat, dui nisi interdum dui, et vulputate diam","key33": " nisi id ligula. Curabitur accumsan dolor eget quam","key34": " sagittis, vitae vestibulum sapien ullamcorper. Nu","key35": "lla pretium risus vel est maximus ultricies. Sed c","key36": "ursus fermentum lacinia. Duis commodo sagittis con","key37": "gue. Phasellus quis metus justo. Fusce pharetra vol","key38": "utpat ipsum id feugiat. Duis vel est lobortis, matt","key39": "is nunc nec, rhoncus felis. Integer et magna non r","key40": "isus auctor rutrum. Vestibulum id libero ut lacus ","key41": "vulputate varius. Integer porta nibh in felis sol","key42": "licitudin tristique. Nulla malesuada sapien libero","key43": " ac maximus. Vestibulum sollicitudin, justo sit am","key44": "et suscipit convallis, nisi nisi scelerisque risus,","key45": " vitae scelerisque ex felis vel nunc. Fusce vitae ","key46": "maximus justo. Donec rutrum vel nisl nec tincidunt","key47": "Sed eget quam justo. Pellentesque habitant morbi tr","key48": "istique senectus et netus et malesuada fames ac tur","key49": "pis egestas. Mauris non mauris quis ipsum fermentu","key50": "m dapibus. Sed at lacus eu risus tristique imperdi","key51": "et vel id nisi. Nullam et lorem quis lectus pharetr","key52": "a in eu elit. Ut sagittis ex in lectus vestibulum,","key53": " nec efficitur urna consequat. Duis placerat a nisi","key54": " nec vehicula. Mauris convallis sem eu efficitur. P","key55": "ellentesque habitant morbi tristique senectus et ne","key56": "tus et malesuada fames ac turpis egestas. Sed in do","key57": "lor vel dui maximus accumsan. Ut luctus quam id neq","key58": "ue venenatis. Vestibulum ante ipsum primis in fauc","key59": "ibus orci luctus et ultrices posuere cubilia curae","key60": "; Curabitur posuere ultricies ex, at tempus ligula ","key61": "gravida et. Sed non enim non leo viverra ullamcorp","key62": "er. Nulla cursus mi odio, id vehicula magna ullamco","key63": "rper nec. Proin quis commodo purus. In vehicula lib","key64": "ero velit, vel molestie nulla sodales nec. Vestibu","key65": "lum ante ipsum primis in faucibus orci luctus et ul","key66": "trices posuere cubilia curae; Donec nec purus in to","key67": "rtor tempor tincidunt. Integer accumsan sagittis m","key68": "etus, nec scelerisque libero vestibulum at. Nam ac ","key69": "suscipit tortor, vitae sollicitudin sapien. Curabi","key70": "tur dictum elementum odio, nec volutpat sapien mol","key71": "estie in. Phasellus semper, justo vitae scelerisque","key72": " sollicitudin, tortor sapien scelerisque tortor, a","key73": "dapibus odio turpis non libero. Vivamus in lacus in","key74": " erat gravida fermentum sit amet sit amet velit. Qu","key75": "isque sed nisi eu tellus faucibus feugiat. Suspendi","key76": "sse fringilla nibh eget quam finibus, non efficitur","key77": " elit rhoncus. Nullam hendrerit, enim vel commodo ","key78": "tempus, velit libero euismod odio, ac ultricies tur","key79": "pis lacus id eros. Ut et nibh eget est blandit dap","key80": "ibus. Aenean nec congue enim, in varius urna. Duis","key81": " nec convallis lorem. Cras nec mauris et orci vulpu","key82": "tate fermentum vitae a sem. Proin euismod aliquam ","key83": "neque non consectetur. Phasellus commodo felis in ","key84": "tincidunt. Pellentesque nec pharetra velit. Nulla ","key85": "vel sagittis odio. Nulla facilisi. In quis commodo","key86": " lectus. Cras ullamcorper suscipit libero, id impe","key87": "rdiet tellus molestie at. Suspendisse potenti. Pell","key88": "entesque eu rhoncus mauris. Cras ac lacus nec nisi ","key89": "efficitur auctor. In pretium non ex euismod interd","key90": "um. Sed quis purus id purus vehicula elementum nec","key91": ". Vivamus vehicula libero vitae massa tempor, vitae","key92": " aliquam ligula efficitur. Aliquam id erat magna. P","key93": "ellentesque habitant morbi tristique senectus et ne","key94": "tus et malesuada fames ac turpis egestas. Integer ","key95": "tus et malesuada fames ac turpis egestas. Integer ","key96": "tus et malesuada fames ac turpis egestas. Integer ","key97": "tus et malesuada fames ac turpis egestas. Integer ","key98": "tus et malesuada fames ac turpis egestas. Integer ","key99": "tus et malesuada fames ac turpis egestas. Integer ","key100": "tus et malesuada fames ac turpis egestas. Integer "}`

// main function
func main() {
	//Check if the number of records to generate is provided
	if len(os.Args) < 2 {
		fmt.Println("Please Provide number of records to generate in the CSV file. Example: go run app.go 1000")
		fmt.Println("To generate a Pre-Load File i.e database preloaded with existing unique data, specify preload. Example: go run app.go 1000 preload")
		return
	}
	//filename
	filename := "product_tokens.csv"

	// Delete the file if it exists
	if _, err := os.Stat(filename); err == nil {
		err := os.Remove(filename)
		if err != nil {
			log.Fatal(err)
		}
	} else if os.IsNotExist(err) {
		// File does not exist, do nothing
	} else {
		log.Fatal(err)
	}

	// Create a new CSV file
	file, err := os.Create(filename)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	writer := csv.NewWriter(file)
	defer writer.Flush()

	// Write CSV header
	header := []string{"Code", "Region", "Serial", "Name", "Values"}
	writeCSVData(writer, header)

	//Number of csv records to generate
	numberOfRecords, _ := strconv.Atoi(os.Args[1])
	preload := false
	if len(os.Args) > 2 && strings.EqualFold(os.Args[2], "preload") {
		preload = true
	}
	fmt.Printf("Starting Product Tokens Utility total records to generate: %d, preload flag: %s \n", numberOfRecords, strconv.FormatBool(preload))
	//loop through the number of records and generate the product tokens
	for i := 1; i < numberOfRecords+1; i++ {
		timestamp := time.Now().UnixNano()
		tokenvalues := `{ "date": "` + strconv.FormatInt(timestamp, 10) + `",` + strings.ReplaceAll(LARGE_TOKEN_STRING, "key", "key-"+strconv.Itoa(i)+"-")
		if preload {
			row := []string{strconv.Itoa(i), REGIONS_PRELOAD[i%len(REGIONS_PRELOAD)], strconv.Itoa(i), "Product_Token_" + strconv.Itoa(i), tokenvalues}
			writeCSVData(writer, row)
		} else {
			row := []string{strconv.Itoa(i), REGIONS[i%len(REGIONS)], strconv.Itoa(i), "Product_Token_" + strconv.Itoa(i), tokenvalues}
			writeCSVData(writer, row)
		}
	}
	fmt.Println("Done Product Tokens Utility generated total records: ", numberOfRecords)
}

// write csv data
func writeCSVData(writer *csv.Writer, header []string) {
	if err := writer.Write(header); err != nil {
		log.Fatalln("error writing record to csv:", err)
	}
}
