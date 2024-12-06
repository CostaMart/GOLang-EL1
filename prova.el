package main

import (
	"fmt"
)

func main() {
       x load "test.csv" Persons
       y load "test.csv" Persons
       x[Age > 30]

       x <<- func (x *Persons) {
	            x.Age = x.Age*2
                }

       fmt.Print(x)
    }

func printStringSlice(slice [][]string) {
    for _, row := range slice {
        for _, value := range row {
            fmt.Printf("%s ", value)
        }
        fmt.Println() // Aggiunge una nuova riga dopo ogni riga di dati
    }
}



