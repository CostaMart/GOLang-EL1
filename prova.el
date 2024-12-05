package main

import (
	"fmt"
)

func main() {

       x load "test.csv" Persons
       x[Age == 40]
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