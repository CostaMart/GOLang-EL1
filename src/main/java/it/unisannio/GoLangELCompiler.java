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
import util.SymbolTableFactory;

public class GoLangELCompiler {
    static final Logger logger = LogManager.getLogger("main");

    public static void main(String[] args) throws IOException {

            try {

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
                        
                        Usage: GoLangELCompiler <file name>.el -<options>
                        
                        Options available: 
                        -s prints symbol table values
                        -o outputDir
                        """);
            return;
            }

            if(args.length > 0) {

                String fileName = args[0];
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

                compileGo(rewriter.getText(), fileName, dir, !cmds.hasOption("go"));

                if(cmds.hasOption("s")){
                    SymbolTableFactory.getInstance().printSymbolTable();
                }

            }

        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }




    }

    public static void compileGo(String goCode, String outputName, String outputDir, boolean delete) throws IOException {
        String currentDir = System.getProperty("user.dir");
        Path startpath = Paths.get(currentDir);

        // genereate exe on windows
        if(System.getProperty("os.name").contains("Windows")) outputName = outputName.concat(".exe");

        // search go in filesystem
        Path path = startpath.resolve("src").resolve("go").resolve("bin").resolve("go");


        // create temporary file and write code
        Path tempFile = Files.createTempFile(startpath, "tempGoCode", ".go");
        Files.write(tempFile, goCode.getBytes());

        // call go compiler
        ProcessBuilder processBuilder = new ProcessBuilder(path.toAbsolutePath().toString(), "build", "-o",
                startpath.toAbsolutePath().resolve(outputName).toAbsolutePath().toString(),
                tempFile.toAbsolutePath().toString());
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