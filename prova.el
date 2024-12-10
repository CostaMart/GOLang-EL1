package main

import (
	"fmt"
	"os"
)

func main() {
    load "test.csv" Person in y

   for x := 0; x < 10; x++ {
     load "test.csv" Person in y
     load "test.csv" Person in y
        fmt.Print(y)
        }

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