package main

import (
	"fmt"
	"os"
)

func main() {
	load "test.csv" Person in y

    for x:= 0; x< 10; x++{


        if x %2 == 0 {
            y <<- printName
        }
    }

}

func printName(p *Person) {
	fmt.Print(p.Name + " ")
}


func sum(cumulated, single Person) (value Person, value2 *Person) {
	value.Age = cumulated.Age + single.Age
		return
}