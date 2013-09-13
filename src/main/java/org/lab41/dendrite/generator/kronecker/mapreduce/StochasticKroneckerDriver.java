package org.lab41.dendrite.generator.kronecker.mapreduce;

import com.thinkaurelius.faunus.FaunusVertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;


import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.lab41.dendrite.generator.kronecker.mapreduce.lib.input.LongSequenceInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a driver for a map-only job that generates a stochastic kronecker graph.
 * <p/>
 * The driver expects the following arguments:
 * <ul>
 * <li>outputPath</li>
 * <li>n - log_2(N) - where N is the number of nodes in the graph (the number of nodes in a graph generator using this</li>
 * method should always be a power of two.
 * <li>t_11, t_12, t_21, t_31 - the stochasic initator matrix. The sum of these variables should be 1.</li>
 * </ul>
 * <p/>
 * <p/>
 * <p/>
 * The arguments should be provided as follows :
 * <p/>
 * StochasticKroneckerDriver outputPath n t_11 t_12 t_21 t_31
 * <p/>
 * This version of the driver uses the {@link LongSequenceInputFormat} as the input format, and the
 * {@link FileOutputFormat} as the output format.
 *
 * @author kramachandran
 */
public class StochasticKroneckerDriver extends Configured implements Tool {
    Path outputPath;
    String initiator;
    int n;
    Logger logger = LoggerFactory.getLogger(StochasticKroneckerDriver.class);

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new StochasticKroneckerDriver(), args);

        System.exit(exitCode);
    }

    protected boolean parseArgs(String[] args) {

        if (args.length != 6) {
            return false;

        }
        else
        {
            //output path
            outputPath = new Path( args[0]);

            //read n
            n = Integer.parseInt(args[1]);
            if( n > 63)
            {
                return false;
            }

            //read the initator matrix
            String t_11 = args[2];
            String t_12 = args[3];
            String t_21 = args[4];
            String t_22 = args[5];

            initiator =  t_11 +", " + t_12 + ", "+ t_21 + ", " + t_22 ;


        }
        return true;

    }

    @Override
    public int run(String[] args) throws Exception {
        if(parseArgs(args))
        {

            Configuration conf = new Configuration();


            /** configure Job **/
            Job job = new Job(getConf());
            job.setJobName("StochasticKronecker");
            job.setJarByClass(StochasticKroneckerDriver.class);

            /** Set the Mapper & Reducer**/
            job.setMapperClass(StochasticKroneckerGeneratorMapper.class);
            //job.setReducerClass(EdgeListToFaunusAnnotatingReducer.class);
            job.setNumReduceTasks(0);

            /* Configure Input Format to be our custom InputFormat */
            job.setInputFormatClass(LongSequenceInputFormat.class);
            job.setOutputFormatClass(SequenceFileOutputFormat.class);
//
            FileOutputFormat.setOutputPath(job, outputPath);

            /* Configure Map Output */
            job.setMapOutputKeyClass(NullWritable.class);
            job.setMapOutputValueClass(FaunusVertex.class);

            /* Configure job (Reducer) output */
            job.setOutputKeyClass(NullWritable.class);
            job.setOutputValueClass(FaunusVertex.class);

            //Set the configuration
            job.getConfiguration().set(Constants.PROBABLITY_MATRIX, initiator);
            job.getConfiguration().set(Constants.N, Integer.toString(n));
            if (job.waitForCompletion(true)) {
                return 0;
            } else {
                return 1;
            }
        }
        else
        {
            System.out.println("Usage : outputPath n t_11 t_12 t_21 t_31 \n" +
                    "               n must be less than 64");
            return 1;
        }


    }
}

