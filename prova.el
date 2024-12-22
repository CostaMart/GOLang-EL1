package main

import (
	"fmt"
	"os"
)

func main() {
    mario := mario()
    load "test.csv" Person in y

    var tr Dataset[Person]
    var tst Dataset[Person]
    split y 0.7 : 0.3 in tr,tst

    train model <- tr classifier

    prediction model <- tst in results


    fmt.Print(results)



}