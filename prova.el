package main

import (
"fmt"
)



func main() {

    x load "test.csv" Person

	var s []int
	s = append(s, 10)
	s = append(s, 10)

	s <<- double

    fmt.Println(s)

	x <<- func (p *Person){
	    p.Age = p.Age*2
	}

	fmt.Println(x)

}

func double (x *int){
    *x = *x*2
}

