package main

import (
	"fmt"
	"os"
)

func main() {
    load "test.csv" Person in y

    var train []Person
    var test []Person
    var validate []Person

    split y 0 : 1 : 0 in train,test,validate

    fmt.Println(train)
    fmt.Println(test)
    fmt.Println(validate)
}




func sum(cumulated, single *Person) (mario Person) {
	mario.Age = cumulated.Age + single.Age
		return
}