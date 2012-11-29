package epics.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/** See Javadoc at constructor for more on params/properties.
 * This is a singleton and can only be instantiated by using
 * setPropertiesFile() and accessed with the static getters. */
public class RunParams {

	/** When specifying numbered params, this is the count of expected params */
	public static final String KEY_NUM_OF_PARAMS = "NumOfParams";
	
	/** When specifying numbered params, each key's key should have this 
	 * prefix before the param number */
	public static final String NUMBERED_PARAM_KEY_PREFIX = "ParamKey_";
	
	/** When specifying numbered params, each value's key should have this 
	 * prefix before the param number */	
	public static final String NUMBERED_PARAM_VALUE_PREFIX = "ParamValue_";
	
    // --------------------- Static ------------------------------
    private static RunParams INSTANCE = null;
    private static RunParams getInstance() {
        if (INSTANCE == null) {
        	System.err.println("Couldn't load Params file. Exiting...");
        	throw new IllegalStateException("RunParams has not been instantiated "
        			+ "with a properties file. Try again with a properties file.");
        }
        return INSTANCE;
    }

    public static void loadFromFile(String filepath) throws IOException {
        setPropertiesFile(filepath);
        System.out.println("Params File loaded");
    }

    public static RunParams setPropertiesFile(String filepath) throws IOException {
        INSTANCE = new RunParams(filepath);
        return INSTANCE;
    }

    /** Convenience method, referring to getString() */
    public static String get(String key) {
    	return getString(key);
    }
    
    public static String getString(String key) {
    	return getInstance().getProperty(key);
    }

    public static Integer getInt(String key) {
        return Integer.parseInt(getInstance().getProperty(key));
    }

    public static Double getDouble(String key) {
        return Double.parseDouble(getInstance().getProperty(key));
    }

    public static Float getFloat(String key) {
        return Float.parseFloat(getInstance().getProperty(key));
    }

    public static Boolean getBool(String key){
        return Boolean.parseBoolean(key);
    }

    public static String getAllPropertiesString() {
        return getInstance().prop.toString();
    }

    /** Sets a property. Returns previous property if existed */
    public static String overrideProperty(String key, String value) {
        return (String) getInstance().prop.setProperty(key, value);
    }
    // --------------------- Static ------------------------------

    

    private Properties prop = new Properties();

    /**
     * Allows parameters/properties for the program to be stored in a properties
     * file (that is, a file with a .properties extension as specified in the Java
     * specification. Format is roughly:
     * - Comments are any lines that start with a #
     * - Properties are line-separated by \r, \n or \r\n
     * - One property per line that looks like "key=value", e.g.
     *    hostname=localhost
     */
    private RunParams(String filepath) throws IOException, FileNotFoundException {
        prop.load(new FileInputStream(filepath));
    }

    /** If a property is contained in the properties file, returns it. Otherwise null */
    private String getProperty(String key) {
        return prop.getProperty(key);
    }
}
