package epics.camwin;

import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 *
 * @author Lukas Esterle <Lukas.Esterle@aau.at> & Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class Main {

    // Input XML file with simulation definition
    static String input_file = null;
    // Output file with all simulation statistics
    static String output_file = "output.csv";
    // Seed
    static long seed = 0;
    // After this many timesteps simulation will stop
    static int simulation_time = 100;
    // No-gui, set this to false to disable gui.
    static boolean showgui = true;
    
    static boolean useGlobal = false;
    
    static String summaryFile = null;
    static String paramFile = null;
    static String algo = "";
    static String comm = "";
    static int predefVG = -1;
    static int trackErr = -1;
    static int camErr = -1;
    static int camReset = 50;

    static void print_parameters() {
        // Nice dashed line on top
        for (int i = 0; i < 28; i++) {
            System.out.print('-');
        }
        System.out.print(" Simulation Parameters ");
        for (int i = 0; i < 29; i++) {
            System.out.print('-');
        }
        System.out.println();

        System.out.println("Input file:      " + input_file);
        System.out.println("Output file:     " + output_file);
        System.out.println("Seed:            " + seed);
        System.out.println("Simulation time: " + simulation_time);

        // Nice dashed line on the bottom as well
        for (int i = 0; i < 80; i++) {
            System.out.print('-');
        }
        System.out.println();
    }

    public static void usage() {
        System.out.println("USAGE: ");
        System.out.println("  program [OPTIONS] input_file");
        System.out.println("\nuse -h for help");
    }

    public static void help() {
        usage();

        System.out.println(
        		
                "OPTIONS:\n" +
                " -h, --help                 Print this help message\n" +
                " -o, --output [STRING]      Change output file name (default: output.csv)\n" +
                " -f, --summaryfile [STRING] Set a summary file to append the summary to\n" +
                " -p, --paramfile [STRING]   Set a parameters file for this run\n" +                
                " -s, --seed [INTEGER]       Used this seed (default: 0)\n" +
                " -t, --time [INTEGER]       Simulation time, in time steps (default: 100)\n" +
                " -g, --global               Uses Global Registration Component\n" +
                " -v, --vg [INTEGER]         Defines the visiongraph\n" +
                "                              -1 = defined in scenario file (default), \n" +
                "                              0 = static as defined in scenario, \n" +
                "                              1 = dynamic - ignore scenario file, \n" +
                "                              2 = dynamic - start with scenario file \n" +
                " -c, --comm [INTEGER]       Defines Communication ((default) 0 = Broadcast, \n" +
                "                              1 = SMOOTH, 2 = STEP, 3 = Static) \n" +
                " -a, --algo [STRING]        Defines the used algorithm ((default) \"active\", \"passive\") \n" +
                "\n" +
                "     --no-gui               Will launch simulator in command line mode\n"

                );
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        // Set the parameters depending on command line options.

        int c;
        String arg;
        LongOpt[] longopts = new LongOpt[14];

        StringBuffer sb = new StringBuffer();
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
        longopts[2] = new LongOpt("summaryfile", LongOpt.REQUIRED_ARGUMENT, null, 'f');
        longopts[3] = new LongOpt("seed", LongOpt.REQUIRED_ARGUMENT, null, 's');
        longopts[4] = new LongOpt("time", LongOpt.REQUIRED_ARGUMENT, null, 't');
        longopts[5] = new LongOpt("no-gui", LongOpt.NO_ARGUMENT, null, 1000);
        longopts[6] = new LongOpt("global", LongOpt.NO_ARGUMENT, null, 'g');
        longopts[7] = new LongOpt("algo", LongOpt.REQUIRED_ARGUMENT, null, 'a');
        longopts[8] = new LongOpt("comm", LongOpt.REQUIRED_ARGUMENT, null, 'c');
        longopts[9] = new LongOpt("vg", LongOpt.REQUIRED_ARGUMENT, null, 'v');
        longopts[10] = new LongOpt("deterr", LongOpt.REQUIRED_ARGUMENT, null, 'd');
        longopts[11] = new LongOpt("camerr", LongOpt.REQUIRED_ARGUMENT, null, 'e');
        longopts[12] = new LongOpt("camreset", LongOpt.REQUIRED_ARGUMENT, null, 'r');
        longopts[13] = new LongOpt("paramfile", LongOpt.REQUIRED_ARGUMENT, null, 'p');
        
        
        Getopt g = new Getopt("guiapp", args, "a:c:v:gho:d:e:r:s:t:f:p:", longopts);
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 0:
                    arg = g.getOptarg();
                    System.out.println("Got long option with value '"
                            + (char) (new Integer(sb.toString())).intValue()
                            + "' with argument "
                            + ((arg != null) ? arg : "null"));

                    break;

                case 1:
                    System.out.println("I see you have return in order set and that "
                            + "a non-option argv element was just found "
                            + "with the value '" + g.getOptarg() + "'");
                    break;

                case 'h':
                    help();
                    System.exit(0);
                    break;

                case 'o':
                    arg = g.getOptarg();
                    System.out.println("Setting output file to: " + arg);
                    output_file = arg;
                    break;

                case 'f':
                    arg = g.getOptarg();
                    System.out.println("Setting summary file: " + arg);
                    summaryFile = arg;
                    break;

                case 's':
                    arg = g.getOptarg();
                    try {
                        seed = Integer.parseInt(arg);
                    } catch (NumberFormatException e) {
                        System.err.println(
                                "Value passed as seed is not a vaild integer: "
                                + arg + " i");
                        System.exit(1);
                    }
                    break;

                case 'p':
                    arg = g.getOptarg();
                    System.out.println("Setting param file: " + arg);
                    paramFile = arg;
                    break;
                    
                case 't':
                    arg = g.getOptarg();
                    try {
                        simulation_time = Integer.parseInt(arg);
                    } catch (NumberFormatException e) {
                        System.err.println(
                                "Value passed as time is not a valid integer: "
                                + arg + " i");
                        System.exit(1);
                    }
                    break;
                case 'g':
                	useGlobal = true;
                	break;
                case 'a':
                	arg = g.getOptarg();
                	algo = arg;
                	break;
                case 'c':
                	arg = g.getOptarg();
                	comm = arg;
                	break;
                case 'd':
                	arg = g.getOptarg();
                	trackErr = Integer.parseInt(arg);
                	break;
                case 'r':
                	arg = g.getOptarg();
                	camReset = Integer.parseInt(arg);
                	break;
                case 'e':
                	arg = g.getOptarg();
                	camErr = Integer.parseInt(arg);
                	break;
                case 'v':
                	arg = g.getOptarg();
                	predefVG = Integer.parseInt(arg);
                	break;
                case 1000: // no-gui
                    showgui = false;
                    break;

                case ':':
                    System.out.println("Doh! You need an argument for option "
                            + (char) g.getOptopt());
                    break;
                //
                case '?':
                    System.out.println("The option '" + (char) g.getOptopt()
                            + "' is not valid");
                    break;
                //
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

        int opt_arg_counter = 0;
        StringBuilder strb = new StringBuilder();
        
        for (int i = g.getOptind(); i < args.length; i++) {
            opt_arg_counter++;
            strb.append(args[i]);
            System.out.println("Non option argv element: " + args[i] + "\n");
        }

        if (opt_arg_counter > 1) {
            System.err.println("Error, I don't understand these parameters '"
                    + strb.toString() + "'");
            usage();
            System.exit(1);
        }

        if (opt_arg_counter == 1) {
            input_file = strb.toString();
        }

        print_parameters();
        SimSettings ss = new SimSettings(algo, comm, predefVG);
        if (input_file == null) {
            System.err.println("Error, no simulation file provided");
            usage();
            System.exit(1);
        	
        }
        else{
	        
	        boolean success = ss.loadFromXML(input_file);
	
	        if (!success) {
	            System.err.println("Error, Could not load " + input_file);
	            System.exit(1);
	        }
        }
        
        SimCore sim = new SimCore(seed, output_file, summaryFile, paramFile, ss, 
        		useGlobal, camErr, camReset, trackErr, false, true);
        if (showgui == false) {
            for (int i = 0; i < simulation_time; i++) {
                sim.update();
            }
            sim.close_files();

        } else {
            sim_model = new SimCoreModel(sim);
            WindowMain win = new WindowMain(sim_model, input_file);
            win.createAndShowGUI();
        }

    }

    public static SimCoreModel sim_model;

}
