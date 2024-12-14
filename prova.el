package main

import (
	"fmt"
	"os"
)

func main() {

    load "test.csv" Person in y

    var train Dataset[Person]
    var test Dataset[Person]

    split y 0.5 : 0.5 in train, test

    train-model model <- train

    test-model model <- test in x

    evaluate-model model <- x | PRECISION | RECALL

    fmt.Print(precision)
    fmt.Print(recall)



}