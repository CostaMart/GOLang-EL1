package main

import "strconv"

func ConvertString(input string) (string, *int, *float64) {
	// Prova a convertire in un intero
	if intValue, err := strconv.Atoi(input); err == nil {
		return "", &intValue, nil
	}

	// Prova a convertire in un float
	if floatValue, err := strconv.ParseFloat(input, 64); err == nil {
		return "", nil, &floatValue
	}

	// Se non è né int né float, restituisci la stringa
	return input, nil, nil
}
