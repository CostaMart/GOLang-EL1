
package main

import (
	"fmt"
	"os"
)

func main() {
	load "test.csv" Person in y

    y <<- printName
}

func printName(p *Person) {
	fmt.Print(p.Name + " ")
}


func sum(cumulated, single Person) (value Person) {
	value.Age = cumulated.Age + single.Age
		return
}