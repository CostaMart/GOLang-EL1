package main

import (
	"fmt"
	"os"
)

func main() {

    load "test.csv" Person in y
    y ->> Age%2 == 0 in x
    fmt.Print(x)

    var z Dataset[Person]
    y <<- ciao in z

}

func ciao(p *Person){
    fmt.Print("ciao")
}