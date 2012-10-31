package consoletester;

//import mycalculator.MyCalculator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 *
 * @author gumer
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        // TODO code application logic here

        System.out.println("Modified!");

        System.out.println("Hello there my dear! :*");
	//System.out.println("2+2=" + MyCalculator.Add( 2, 3 ) );

	Main m = new Main();


	File file = new File("Algorithms.jar");   
	String lcStr = "alg1.Algorithm1";   
	URL jarfile = new URL("jar", "","file:" + file.getAbsolutePath()+"!/");


	System.out.println("000");
	ClassLoader loader = URLClassLoader.newInstance(
							new URL[] { jarfile },
							m.getClass().getClassLoader()
							);
	System.out.println("AAAA");
	Class<?> clazz = Class.forName(lcStr, true, loader);
	System.out.println("BBB");
	Class<? extends Runnable> runClass = clazz.asSubclass(Runnable.class);
	System.out.println("CCC");
	// Avoid Class.newInstance, for it is evil.
	Constructor<? extends Runnable> ctor = runClass.getConstructor();
	Runnable doRun = ctor.newInstance();
	doRun.run();



	/*

	System.out.println("000");
	URLClassLoader child = URLClassLoader.newInstance(new URL[] {jarfile });

	System.out.println("AAAA");

	//URLClassLoader child = new URLClassLoader (new URL[]{ new URL("file:./Algorithms.jar")}, m.getClass().getClassLoader());
	//Class classToLoad = Class.forName (lcStr, true, child);
	Class classToLoad = child.loadClass(lcStr);

	System.out.println("BBB");

	Method method = classToLoad.getDeclaredMethod ("Execute");
	Object instance = classToLoad.newInstance ();
	Object result = method.invoke (instance);

	System.out.println("Result: " + result.toString() );

	*/




    }

}
