package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.apache.commons.cli.*;

import java.io.IOException;

public class BenchmarkCmdMain {

    public static void main(String[] args)  {
        Options options = new Options();
        Option baseDirectory = new Option("d", "dir", true, "source and target file directory");
        baseDirectory.setRequired(true);
        options.addOption(baseDirectory);
        Option sourceFile = new Option("s", "source", true, "source file name");
        sourceFile.setRequired(true);
        options.addOption(sourceFile);
        Option targetFile = new Option("t", "target", true, "target file name");
        targetFile.setRequired(true);
        options.addOption(targetFile);
        Option threads = new Option("threads", true, "number of threads");
        threads.setRequired(true);
        options.addOption(threads);
        Option multiBenchmark = new Option("multi", false, "benchmark with all files starting with the source file name and target file name");
        multiBenchmark.setOptionalArg(true);
        options.addOption(multiBenchmark);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine a = parser.parse(options, args);
            String baseDir = a.getOptionValue(baseDirectory);
            String sf = a.getOptionValue(sourceFile);
            String tf = a.getOptionValue(targetFile);
            int t = Integer.parseInt(a.getOptionValue(threads));
            boolean multi = a.hasOption(multiBenchmark);

            if(multi){
                MultiFileBenchmark.test(baseDir, sf, tf, t);
            }else{
                Benchmark.test(baseDir, sf, tf, t);
            }

        } catch (ParseException e){
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
