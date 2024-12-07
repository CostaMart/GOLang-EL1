import org.junit.Test;
import static org.junit.Assert.*;
import it.unisannio.GoLangELCompiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestGoCompiler {


    @Test
    public void testLoadCsv() {

        try {
            File dir = new File(".");
            File f = File.createTempFile("loadCsv", ".el");

            try (FileWriter fw = new FileWriter(f)) {
                // Scrive il contenuto nel file
                fw.write("""
                        
                         package main
    
                         import (
                            "fmt"
                            "os"
                         )
    
                         func main() {
                           load "test.csv" Person in y
                           var x Person
                           
                           // test filter
                           y[Age % 2 == 0]
                           
                           // test map
                           y <<- func(p *Person){ fmt.Print(p.Name + " ") }
                              fmt.Println()
                              
                           // test map with declared function   
                           y <<- printName
                             fmt.Println()
                            
                           // test reduce with declared function
                           y reduce sum in x
                           fmt.Print(y)
                         }
                         
                        func sum(cumulated, single *Person) (value Person) {
                            value.Age = (cumulated.Age + single.Age)/2
                            return
                        }
                        
                         func printName(p *Person){
                             fmt.Print(p.Name + " ")
                         }
                        """);
            }

            GoLangELCompiler.main(new String[]{"build", f.getPath().toString()});
            f.delete();

            boolean correct = false;

            for (File file : dir.listFiles()) {
                System.out.println(file.getName());
                if (file.getName().contains("main")) {
                    correct = true;
                    file.delete();
                }
            }

            assertTrue(correct);

            // dai il tempo di cancellare il file
            Thread.sleep(1000);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



}





