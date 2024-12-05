package main

import (
	"fmt"
)

func main() {
        x load "test.csv"
        x[Age == 30]
        printStringSlice(x)
    }

func printStringSlice(slice [][]string) {
    for _, row := range slice {
        for _, value := range row {
            fmt.Printf("%s ", value)
        }
        fmt.Println() // Aggiunge una nuova riga dopo ogni riga di dati
    }
}