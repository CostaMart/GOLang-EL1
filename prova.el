package main

import (
	"fmt"
	"os"
)

func main() {

    load "test.csv" Person in y
    y ->> Age%2 == 0 in x
    fmt.Print(x)
}