package it.unisannio;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import generated.GoLexer;
import generated.GoParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.apache.commons.cli.*;

import java.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.SymbolTable;
import util.SymbolTableFactory;

public class GoLangELCompiler {
    static final Logger logger = LogManager.getLogger("main");
    static String toDo = "run";

    public static void main(String[] args) throws IOException {

            try {
                // clear symboltable
                SymbolTable symbolTable = new SymbolTable();
                symbolTable.clear();

            // setup command line intepreter
            Options options = new Options();
            options.addOption("o", "outputDir", true, "Directory containing output files");
            options.addOption("s", "symbol", false, "prints symbol table values");
            options.addOption("go", "golang", false, "prints symbol table values");
            options.addOption("help", "help", false, "help");

            CommandLineParser cmdParser = new DefaultParser();
            CommandLine cmds = cmdParser.parse(options, args);

            if(cmds.hasOption("-help")){
                System.out.println("""
                        
                        Usage: GoLangELCompiler run | build <file name>.el -<options>
                        
                        Options available: 
                        -s prints symbol table values
                        -o outputDir
                        """);
            return;
            }

            if(args.length > 1) {

                toDo = args[0];
                String fileName = args[1];
                if(!(fileName.endsWith(".el"))) throw new IllegalArgumentException(".el file expected");

                CharStream file = CharStreams.fromFileName(fileName);
                GoLexer lex = new GoLexer(file);

                // setup rewriter
                CommonTokenStream tokens = new CommonTokenStream(lex);
                TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

                GoParser parser = new GoParser(tokens);


                GoParser.SourceFileContext context = parser.sourceFile();
                GoLangELVisitor visit = new GoLangELVisitor(rewriter);
                visit.visit(context);

                // recuperato il programma modificato e stampalo.
                logger.debug("main: DEBUG PRINT\n{}", rewriter.getText());



                fileName = fileName.replace(".el", "");
                String dir;

                if(cmds.hasOption("-o")) dir = cmds.getOptionValue("-o");
                else dir = System.getProperty("user.dir");

                logger.debug(dir);

                compileGo(rewriter.getText(), fileName, dir, !cmds.hasOption("go"), toDo);

                if(cmds.hasOption("s")){
                    SymbolTableFactory.getInstance().printSymbolTable();
                }

            }
            else{
                System.err.println("Usage: GoLangELCompiler run | build <file name>");
            }

        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }




    }

    public static void compileGo(String goCode, String outputName, String outputDir, boolean delete, String toDo) throws IOException {
        String currentDir = System.getProperty("user.dir");
        Path startpath = Paths.get(currentDir);

        // genereate exe on windows
        if(System.getProperty("os.name").contains("Windows")) outputName = outputName.concat(".exe");

        // search go in filesystem
        Path path = startpath.resolve("src").resolve("go").resolve("bin").resolve("go");


        // create temporary file and write code
        Path tempFile = Files.createTempFile(startpath, "tempGoCode", ".go");
        Files.write(tempFile, goCode.getBytes());
        ProcessBuilder processBuilderFormat = new ProcessBuilder(path.toAbsolutePath().toString(), "fmt");
        Process formatter = processBuilderFormat.start();
        try {
            formatter.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        // call go compiler
        logger.debug("compileGo: " + path.toAbsolutePath().toString());
        ProcessBuilder processModeInit = new ProcessBuilder(path.toAbsolutePath().toString(), "mod", "init", "main");
        Process modInit = processModeInit.start();

        try {
            logger.debug("waiting for mod creation");
            modInit.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(path.toAbsolutePath().toString(), toDo);
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // delete tempfile
        if (delete) {
            Files.delete(tempFile);
        }
    }

}