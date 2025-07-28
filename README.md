# Golang EL1 - Golang Extension for Learning v1

## Overview

**Golang EL1** is a didactic compiler extension project developed as part of the "Paradigmi e Linguaggi di Programmazione" course at the University of Sannio. It extends the Go programming language with functional, dataset-oriented, and machine learning constructs while maintaining full compatibility with standard Go.

The compiler is implemented in Java using ANTLR and supports modular parsing, semantic analysis, and code generation phases, following compiler design principles taught during the course.

---

## 🔧 Key Features

### Functional Constructs

- **Map**: Applies a function to each element in a collection.
- **Reduce**: Performs a reduction operation over a collection.

### Dataset Manipulation Constructs

- **Load CSV**: Loads a CSV file into a typed dataset variable.
- **Filter Dataset**: Filters a dataset based on an expression.
- **Split Dataset**: Randomly splits a dataset into multiple parts.
- **Get Row/Column**: Accesses specific elements, rows, or columns of a dataset.

### Declarative ML Constructs

- **Train Model**: Trains a model using KNN or KMeans.
- **Predict**: Performs inference using a trained model.
- **Evaluate**: Evaluates model predictions.

All ML constructs are compatible with the GoLearn library.

---

## 🛠 Implementation Details

- **ANTLR**: Used to generate the parser and manage syntax analysis.
- **Java**: Language of implementation.
- **Visitor Pattern**: Used for parse tree traversal and code generation.
- **Hierarchical Symbol Table**: For variable scoping and semantic checks.
- **Deferred Semantic Checks**: Handles forward references, such as function declarations used before definition.

---

## ⚙️ Compilation Workflow

```text
          +---------+         +---------+         +-------------+
          | PARSER  |  --->   | VISITOR |  --->   | GO COMPILER |
          +---------+         +---------+         +-------------+
         Parse Tree         Intermediate             Machine
         & Symbol Table        Go Code               Code
```

- Independent semantic and translation phases
- Rewriter used to generate clean Go code

---

## 🧪 Sample Constructs

### Load CSV

```go
load "people.csv" Person in x
```

Loads a CSV into variable `x` with row type `Person`.

### Filter Dataset

```go
x filter Age%2 == 0 in y
```

Filters dataset `x`, keeping only even-aged entries into `y`.

### Map

```go
y map func(p *Person){p.Age = p.Age *2 } in x
```

Applies a function to each element of `y`, storing results in `x`.

### Reduce

```go
y reduce sum in x
```

Uses function `sum` to reduce dataset `y`, result stored in `x`.

### Train, Predict, Evaluate

```go
train model <- tr classifier | k:10
predict model <- tst in predictions
evaluate predictions <- tst | precision
```

### Clustering with KMeans

```go
train predictions <- x cluster | partitions:"5"
```

---

## 📁 Example Dataset

```csv
Age,City,Employed
25,New York,true
30,San Francisco,false
22,Los Angeles,true
45,Chicago,true
```

Becomes:

```go
type Person struct {
  Age int
  City string
  Employed bool
}
```

---

## 📚 Compiler Concepts Practiced

- Contextual scoping with stack-based scope tracking
- Shadowing resolution through hierarchical symbol table
- Deferred semantic checks to handle top-down parsing limitations

---

## 👥 Development Team

Developed as a student project by:

- Costantino Martingetti
- Alessandro Repola

Under the guidance of **Prof.ssa Maria Tortorella**.

---

## 🎓 Course Context

Project completed as part of the **Programming Paradigms and Languages** course at **Università degli Studi del Sannio**, 2025.

**Final Grade: Maximum score (30L)**

---

## 📜 License

This project is for educational purposes and does not include a formal license.

---

## 📎 Links

- [ANTLR Project Page](https://www.antlr.org/)
- [GoLearn ML Library](https://github.com/sjwhitworth/golearn)

---

## 🚀 Getting Started

To try out the compiler:

1. Clone the repository
2. Compile using a Java IDE or build tool
3. Feed a `.goel1` file containing extended syntax
4. The generated Go code can then be compiled normally with `go build`

---

