package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.apache.commons.cli.*;

import java.io.IOException;

public class BenchmarkCmdMain {

    public static void main(String[] args) {
        Options options = new Options();
        Option baseDirectory = new Option("d", "dir", true, "source and target file directory");
        baseDirectory.setRequired(true);
        options.addOption(baseDirectory);
        Option sourceFile = new Option("s", "source", true, "source file name");
        sourceFile.setRequired(true);
        options.addOption(sourceFile);
        Option output = new Option("o", "output", true, "output file path");
        output.setRequired(true);
        options.addOption(output);
        Option targetFile = new Option("t", "target", true, "target file name");
        targetFile.setRequired(true);
        options.addOption(targetFile);
        Option threads = new Option("threads", true, "number of threads");
        threads.setRequired(true);
        options.addOption(threads);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine a = parser.parse(options, args);
            String baseDir = a.getOptionValue(baseDirectory);
            String sf = a.getOptionValue(sourceFile);
            String tf = a.getOptionValue(targetFile);
            String outputFile = a.getOptionValue(output);
            int t = Integer.parseInt(a.getOptionValue(threads));

            Benchmark.test(baseDir, sf, tf, t, outputFile);


        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch (org.locationtech.jts.io.ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
