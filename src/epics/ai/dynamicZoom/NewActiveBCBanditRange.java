 package epics.ai.dynamicZoom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import epics.ai.ActiveAINodeMulti;
import epics.camsim.core.Location;
import epics.common.AbstractAINode;
import epics.common.IBanditSolver;
import epics.common.ICameraController;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;
import epics.common.StatsPerZoom;

public class NewActiveBCBanditRange extends ActiveAINodeMulti{

        List<StatsPerZoom> stats = new ArrayList<StatsPerZoom>();
        
        int BANDITINTERVALL = 10;
        int banditI = 0;
        
        IBanditSolver bandit;
        double rangeDisc;
        
        int _shared;
        int _yours;
        int _private;

//        private double _accumTrackedProporation;

//        private double _accumTotalConf;

        private Map<Location, Double> handoverLocationsAndAmount;
        private Map<Location, Double> noBidLocationsAndAmount;
        private Map<Location, Double> olLocationsAndAmount;
        
        protected List<List<Double>> tmpAllProp;//= new ArrayList<ArrayList<Double>>(); //a list of the collected proportion over time for each arm. allProp.get(i) gets the list of proportions at the i-th arm
        protected List<List<Double[]>> tmpAllConf; //= new ArrayList<ArrayList<Double[]>>(); //a list of the collected confidences per object over time for each arm. allConf.get(i) gets the list of confidences (where each item represented by an array of confidences) at the i-th arm
//        protected List<List<Double>> tmpAllOverlap; // = new ArrayList<ArrayList<Double>>(); //a list of the collected overlap over time for each arm. allOverlap.get(i) gets the list of overlaps at the i-th arm
        protected List<List<Double[]>> tmpAllOverlap;
        
        protected List<List<Double>> allProp;//= new ArrayList<ArrayList<Double>>(); //a list of the collected proportion over time for each arm. allProp.get(i) gets the list of proportions at the i-th arm
        protected List<List<Double[]>> allConf; //= new ArrayList<ArrayList<Double[]>>(); //a list of the collected confidences per object over time for each arm. allConf.get(i) gets the list of confidences (where each item represented by an array of confidences) at the i-th arm
//        protected List<List<Double>> allOverlap;
        protected List<List<Double[]>> allOverlap;
        
        
        public NewActiveBCBanditRange(AbstractAINode ai) {
            super(ai);
            rangeDisc = bandit.getTotalArms().length;
            _shared = ai.getHandoverLocations().size() + ai.getOverlapLocation().size();
            _yours = 0;
            _private = ai.getNoBidLocations().size();
//            _accumTrackedProporation = ai.getTrackedProportion();
//            _accumTotalConf = ai.getTotalConfidence();
            handoverLocationsAndAmount = ai.getHandoverLocations();
            noBidLocationsAndAmount = ai.getNoBidLocations();
            olLocationsAndAmount = ai.getOverlapLocation();
         }

//        public BanditRange(boolean staticVG, Map<String, Double> vg,
//                IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
//            super(staticVG, vg, r, auctionDuration, rg);
//            
//        }

        public NewActiveBCBanditRange(boolean staticVG, Map<String, Double> vg,
                IRegistration r, int auctionDuration, RandomNumberGenerator rg,
                IBanditSolver bs) {
            super(staticVG, vg, r, auctionDuration, rg, bs);
            bandit = bs;
            rangeDisc = bandit.getTotalArms().length;
            _shared = 0;
            _yours = 0;
            _private = 0;
//            _accumTrackedProporation = 0.0;
//            _accumTotalConf = 0.0;
            handoverLocationsAndAmount = new HashMap<Location, Double>();
            noBidLocationsAndAmount = new HashMap<Location, Double>();
            olLocationsAndAmount = new HashMap<Location, Double>();
            
            tmpAllProp = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
            tmpAllConf = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
//            tmpAllOverlap = new ArrayList<List<Double>>(bandit.getTotalArms().length); v
            tmpAllOverlap = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
            
            
            allProp = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
            allConf = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
//            allOverlap = new ArrayList<List<Double>>(bandit.getTotalArms().length);
            allOverlap = new ArrayList<List<Double[]>>(bandit.getTotalArms().length);  
           
            
            for(int i = 0; i < bandit.getTotalArms().length; i++){
                tmpAllProp.add(new ArrayList<Double>()); 
                tmpAllConf.add(new ArrayList<Double[]>()); 
//                tmpAllOverlap.add(new ArrayList<Double>());
                tmpAllOverlap.add(new ArrayList<Double[]>());
                
                allProp.add(new ArrayList<Double>()); 
                allConf.add(new ArrayList<Double[]>()); 
//                allOverlap.add(new ArrayList<Double>());
                allOverlap.add(new ArrayList<Double[]>());
            }
        }

//        public BanditRange(boolean staticVG, Map<String, Double> vg,
//                IRegistration r, RandomNumberGenerator rg) {
//            super(staticVG, vg, r, rg);
//        }

        public NewActiveBCBanditRange(boolean staticVG, Map<String, Double> vg,
                IRegistration r, RandomNumberGenerator rg, IBanditSolver bs) {
            super(staticVG, vg, r, rg, bs);
            bandit = bs;
            rangeDisc = bandit.getTotalArms().length;
            _shared = 0;
            _yours = 0;
            _private = 0;
//            _accumTrackedProporation = 0.0;
//            _accumTotalConf = 0.0;
            handoverLocationsAndAmount = new HashMap<Location, Double>();
            noBidLocationsAndAmount = new HashMap<Location, Double>();
            olLocationsAndAmount = new HashMap<Location, Double>();
            
            tmpAllProp = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
            tmpAllConf = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
//            tmpAllOverlap = new ArrayList<List<Double>>(bandit.getTotalArms().length);
            tmpAllOverlap = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
            
            allProp = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
            allConf = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
//            allOverlap = new ArrayList<List<Double>>(bandit.getTotalArms().length);
            allOverlap = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
            
            
            for(int i = 0; i < bandit.getTotalArms().length; i++){
                tmpAllProp.add(new ArrayList<Double>()); 
                tmpAllConf.add(new ArrayList<Double[]>()); 
//                tmpAllOverlap.add(new ArrayList<Double>());
                tmpAllOverlap.add(new ArrayList<Double[]>());
                
                allProp.add(new ArrayList<Double>()); 
                allConf.add(new ArrayList<Double[]>()); 
//                allOverlap.add(new ArrayList<Double>());
                allOverlap.add(new ArrayList<Double[]>());
            }
        }
        
        @Override
        public void update(){
            super.update();
            banditI ++;
            int strat = bandit.getCurrentStrategy();
            double interv = camController.getMaxRange() / rangeDisc;
            if(strat == -1){
                strat = (int) Math.floor(camController.getRange() / interv); 
            }
            
            if (_trackedProporation >= 0){
                tmpAllProp.get(strat).add(_trackedProporation);
                allProp.get(strat).add(_trackedProporation);
            }
            
            int totPoints = _shared + _yours + _private;
            if(totPoints > 0){
//                tmpAllOverlap.get(strat).add(new Double((_shared)/totPoints));
//                allOverlap.get(strat).add(new Double((_shared)/totPoints));
                
                tmpAllOverlap.get(strat).add(new Double[]{(double) _shared, (double) _yours, (double) _private});
                allOverlap.get(strat).add(new Double[]{(double) _shared, (double) _yours, (double) _private});
            }
            
            if(_confidences.length > 0){
                tmpAllConf.get(strat).add(_confidences);
                allConf.get(strat).add(_confidences);
            }
            
            if(BANDITINTERVALL < banditI){
                bandit.setRewardForStrategy(strat, this.calculateReward());
                
                //use bandit            
                int res = bandit.selectActionWithoutReward();
                
                //change range
                camController.increaseRange(((res+1)*interv)-camController.getRange());
                
                //clear stats
//                _shared = 0;
//                _yours = 0;
//                _private = 0;
//                _accumTrackedProporation = 0;
//                _accumTotalConf = 0;
                
                tmpAllProp = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
                tmpAllConf = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
//                tmpAllOverlap = new ArrayList<List<Double>>(bandit.getTotalArms().length); 
                tmpAllOverlap = new ArrayList<List<Double[]>>(bandit.getTotalArms().length); 
                
                for(int i = 0; i < bandit.getTotalArms().length; i++){
                    tmpAllProp.add(new ArrayList<Double>()); 
                    tmpAllConf.add(new ArrayList<Double[]>()); 
//                    tmpAllOverlap.add(new ArrayList<Double>());
                    tmpAllOverlap.add(new ArrayList<Double[]>());
                }
                
                banditI = 0;
            }
//            _accumTrackedProporation += _trackedProporation;
//            _accumTotalConf += _totalConfidence;
            
            _shared = 0;
            _yours = 0;
            _private = 0;
            
        }
        
        protected double calculateReward() {
            double res = 0.0;
            int totPoints = _shared + _yours + _private;
            
            int strat = bandit.getCurrentStrategy();
            double interv = camController.getMaxRange() / rangeDisc;
            if(strat == -1){
                strat = (int) (camController.getRange() / interv); 
            }
            
            double tmpTrackProp = 0.0;
            for(int i = 0; i < tmpAllProp.get(strat).size(); i++){
                tmpTrackProp += tmpAllProp.get(strat).get(i);
            }
            
            double tmpOverlap = 0.0;
            for(int i = 0; i < tmpAllOverlap.get(strat).size(); i++){
                Double[] d = tmpAllOverlap.get(strat).get(i);
                double tot = d[0] + d[1] + d[2]; 
                tmpOverlap += (d[0] / tot);
            }
            
            double tmpConf = 0.0;
            int tmpObjects = 0;
            for(int i = 0; i < tmpAllConf.get(strat).size(); i++){
                Double[] tmpConfsPS = tmpAllConf.get(strat).get(i);
                for(int j = 0; j < tmpConfsPS.length; j++){
                    tmpConf += tmpConfsPS[j];
                }
                tmpObjects += tmpConfsPS.length;
            }
            
            res = (bandit.getAlpha() * ((tmpAllProp.get(strat).size() > 0) ? (tmpTrackProp/tmpAllProp.get(strat).size()) : 0.0)) + (bandit.getBeta() * ((tmpAllOverlap.get(strat).size() > 0) ? (tmpOverlap/tmpAllOverlap.get(strat).size()) : 0.0)) + (bandit.getGamma() * ((tmpObjects > 0) ? (tmpConf/tmpObjects) : 0.0)); 
            
            return res;
        }
        
        @Override
        public IBanditSolver getBanditSolver(){
            return null;
        }


        @Override
        protected void noBids(ITrObjectRepresentation tor){
            if(camController.getVisibleObjects_bb().get(tor) != null){
                Location l = tor.getLocation();
                if(noBidLocationsAndAmount.containsKey(l)){
                    double amount = noBidLocationsAndAmount.get(l);
                    noBidLocationsAndAmount.put(l, amount+1.0d);
                }
                else{
                    noBidLocationsAndAmount.put(l, 1.0d);
                }
                _private++;
            }
        }
        
        @Override
        protected void overlappingLocation(ITrObjectRepresentation tor){
            if(camController.getVisibleObjects_bb().get(tor) != null){
                Location l = tor.getLocation();
                if(olLocationsAndAmount.containsKey(l)){
                    double amount = olLocationsAndAmount.get(l);
                    olLocationsAndAmount.put(l, amount+1.0d);
                }
                else{
                    olLocationsAndAmount.put(l, 1.0d);
                }
                _shared++;
            }
        }
        
        @Override
        protected void handedOver(ITrObjectRepresentation obj, ICameraController cam){
            Location l = obj.getLocation();
            if(handoverLocationsAndAmount.containsKey(l)){
                double amount = handoverLocationsAndAmount.get(l);
                handoverLocationsAndAmount.put(l, amount+1.0d);
            }
            else{
                handoverLocationsAndAmount.put(l, 1.0d);
            }
            if(camController.getVisibleObjects_bb().get(obj) != null){
                _shared ++;
            }
            else{
                _yours++;
            }
        }
        
        /**
         * If camera can see object but lost auction (not tracked).
         */
        @Override
        protected Object handle_stopSearch(String from, ITrObjectRepresentation content) {
            super.handle_stopSearch(from, content);
            if(camController.getVisibleObjects_bb().get(content) != null){
                if(!isTracked(content)){
                    _shared ++;
                }
            }
            return null;
        }
        
       

        @Override
        public Map<Location, Double> getHandoverLocations() {
            return handoverLocationsAndAmount;
        }

        @Override
        public Map<Location, Double> getNoBidLocations() {
            return noBidLocationsAndAmount;
        }

        @Override
        public Map<Location, Double> getOverlapLocation() {
            return olLocationsAndAmount;
        }
        
    }

