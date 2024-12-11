package main

import (
	"fmt"
	"os"
)

func main() {

    load "test.csv" Person in y

    var train Dataset[Person]
    var test Dataset[Person]

    split y 1.0 : 0.0 in train, test
    train-model model <- train

    fmt.Println(test)
    fmt.Println(train)
    fmt.Println(model)
    fmt.Println("mario")

}