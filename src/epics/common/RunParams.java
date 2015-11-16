package epics.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/** See Javadoc at constructor for more on params/properties.
 * This is a singleton and can only be instantiated by using
 * setPropertiesFile() and accessed with the static getters. */
public class RunParams {

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

    /** 
     * Load all parameters from this properties file and dump all previous
     * properties (no current support for loading properties from multiple files).
     * @param filepath
     * @throws IOException
     */
    public static void loadFromFile(String filepath) throws IOException {
        setPropertiesFile(filepath);
        System.out.println("Params File loaded");
    }
    
    /** 
     * Loads the given file if no params have been loaded from any file at all
     * @param filepath
     * @return
     * @throws IOException
     */
    public static boolean loadIfNotLoaded(String filepath) throws IOException {
    	if (INSTANCE == null) {
    		loadFromFile(filepath);
    		return true;
    	}
    	return false;
    }

    /**
     * 
     * @param filepath
     * @return
     * @throws IOException
     */
    public static RunParams setPropertiesFile(String filepath) throws IOException {
        INSTANCE = new RunParams(filepath);
        return INSTANCE;
    }

    /** 
     * Convenience method, referring to getString()
     * @param key
     * @return
     */
    public static String get(String key) {
    	return getString(key);
    }
    
    /**
     * 
     * @param key
     * @return
     */
    public static String getString(String key) {
    	return getInstance().getProperty(key);
    }

    /**
     * 
     * @param key
     * @return
     */
    public static Integer getInt(String key) {
        return Integer.parseInt(getInstance().getProperty(key));
    }

    /**
     * 
     * @param key
     * @return
     */
    public static Double getDouble(String key) {
        return Double.parseDouble(getInstance().getProperty(key));
    }

    /**
     * 
     * @param key
     * @return
     */
    public static Float getFloat(String key) {
        return Float.parseFloat(getInstance().getProperty(key));
    }

    /**
     * 
     * @param key
     * @return
     */
    public static Boolean getBool(String key){
        return Boolean.parseBoolean(key);
    }
    
    /**
     * 
     * @return
     */
    public static String getAllPropertiesString() {
        return getInstance().prop.toString();
    }

    /** 
     * Returns set of entries with type 'Object, Object', but keys  
     * should be cast to Strings and values to their appropriate type.
     * Order not guaranteed!
     * @return
     */
    public static Set<Entry<Object, Object>> getAllProperties() {
    	return getInstance().prop.entrySet();
    }
    
    /** 
     * Sets a property. Returns previous property if existed 
     * @param key
     * @param value
     * @return
     */
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
     * @param filepath 
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    private RunParams(String filepath) throws IOException, FileNotFoundException {
        prop.load(new FileInputStream(filepath));
    }

    /** If a property is contained in the properties file, returns it. Otherwise null 
     * @param key 
     * @return */
    private String getProperty(String key) {
        return prop.getProperty(key);
    }
}
