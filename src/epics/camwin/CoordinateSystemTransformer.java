package epics.camwin;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class CoordinateSystemTransformer {

    private double sim_min_x;
    private double sim_max_x;
    private double sim_min_y;
    private double sim_max_y;
    private double window_width;
    private double window_height;
    private double use_win_height;
    private double use_win_width;

    public CoordinateSystemTransformer(
            double sim_min_x, double sim_max_x,
            double sim_min_y, double sim_max_y,
            double window_width, double window_height ){

        this.sim_min_x = sim_min_x;
        this.sim_max_x = sim_max_x;
        this.sim_min_y = sim_min_y;
        this.sim_max_y = sim_max_y;
        this.window_height = window_height;
        this.window_width = window_width;

    }

    private void updateUsed(){
        double sim_width = this.sim_max_x - this.sim_min_x;
        double sim_height = this.sim_max_y - this.sim_min_y;

        double sim_aspect_ratio = sim_width / sim_height;
        double win_aspect_ratio = this.window_width / this.window_height;

        //System.out.println( "sim_aspect_ratio " + sim_aspect_ratio);
        //System.out.println( "win_aspect_ratio " + win_aspect_ratio);

        if ( sim_aspect_ratio > win_aspect_ratio ){
            this.use_win_height = this.window_width / sim_aspect_ratio;
            this.use_win_width = this.window_width;
        }
        else{
            this.use_win_height = this.window_height;
            this.use_win_width = this.window_height * sim_aspect_ratio;
        }
    }

    public void setWindowHeight( double window_height ){
        this.window_height = window_height;
        this.updateUsed();
    }

    public void setWindowWidth( double window_width ){
        this.window_width = window_width;
        this.updateUsed();
    }

    public double winToSimX(double win_x){
    	double sim_width = this.sim_max_x - this.sim_min_x;
    	double resX = (win_x * sim_width) / use_win_width;
    	return resX + this.sim_min_x;
    	//return sim_width; // (xx * sim_width) / use_win_width;// (xx * sim_width)/this.use_win_width + this.sim_min_x;
    }
    
    public double winToSimY(double win_y){
    	double sim_height = this.sim_max_y - this.sim_min_y;
    	double resY = (win_y * sim_height) / this.use_win_height;
        return resY + this.sim_min_y - 5;
        
        //return resY;// - (resY + this.sim_min_y);
    }
    
    public double simToWindowX( double sim_x ){
        double sim_width = Math.abs(this.sim_max_x) + Math.abs(this.sim_min_x);
        double xx = sim_x + Math.abs(this.sim_min_x);
        return (xx * this.use_win_width) / sim_width;
    }

    public double simToWindowY( double sim_y ){
        double sim_height = Math.abs(this.sim_max_y) + Math.abs(this.sim_min_y);
        double yy = sim_y + Math.abs(this.sim_min_y);
        double result = (yy * this.use_win_height) / sim_height;
        return this.use_win_height - result;
    }
    
    public double toCenterBasedX(double sim_x){
        return sim_x - sim_max_x;
    }
    
    public double toCenterBasedY(double sim_y){
        return sim_max_y - sim_y;
    }
    
    public int getRealWidth() {
        
        return (int) (simToWindowX(sim_max_x) + simToWindowX(sim_min_x));
    }

    public int getRealHeight() {
        return (int) (simToWindowY(sim_max_y) + simToWindowY(sim_min_y));
    }

}
