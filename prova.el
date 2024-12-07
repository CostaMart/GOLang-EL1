
package main

import (
	"fmt"
	"os"
)

func main() {
	load "test.csv" Person in y

	var x []Person
    y <<- male in x


    fmt.Println(x)
    fmt.Print(y)
}

func printName(p *Person){
	fmt.Print(p.Name + " ")
}

func sum(cumulated, single *Person) (value Person) {
	value.Age = (cumulated.Age + single.Age)/2
	return
}

func male(p *Person){ p.Age = p.Age *2 }