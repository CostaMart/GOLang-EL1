package main

import (
	"fmt"
	"os"
)

func main() {
	load "test.csv" Person in y

    y <<- printName

}

func printName(p *Person) (value Person){
	fmt.Print(p.Name + " ")
}


func sum(cumulated, single Person) (value Person, value2 *Person) {
	value.Age = cumulated.Age + single.Age
		return
}