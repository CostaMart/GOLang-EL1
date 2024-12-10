package main

import (
	"fmt"
	"os"
)

func main() {
    load "test.csv" Person in y
    var x Person
    y reduce func(cumulated, single *Person) (mario Person) {
                    mario.Age = cumulated.Age + single.Age
             		return
             } in x

    fmt.Print(x)
}




func sum(cumulated, single *Person) (mario Person) {
	mario.Age = cumulated.Age + single.Age
		return
}