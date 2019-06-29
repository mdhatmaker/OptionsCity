import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.InstrumentDetails;
import com.optionscity.freeway.api.VolatilityCurve;
import com.optionscity.freeway.api.IGrid;

/*
   Job that basically goes through all months for all configured instruments and sets a flat skew for each instrument
*/
public class VolCurve extends AbstractJob {

	IGrid volGrid;

	class ProductData {
		String productName;
		SortedSet<Double> strikeSet = new TreeSet<Double>();
		double[] vols;
		double[] strikes;

		ProductData(String productName) {
			this.productName = productName;
		}
	} // class ProductData

	String insts, instMonth;
	double inputVol;
	//SortedSet<Double> strikeSet=new TreeSet<Double>();

	double[] vols;
	double[] strikes;
	//Double[] strikesObj;
	
	/**** INSTALL ****/
	public void install(IJobSetup setup){
		setup.setDefaultDescription("Sets a flat skew at a given level");
		setup.addVariable("instruments","instruments to filter on","instruments","OZS;O;;;;;");
		setup.addVariable("instMonth","month string to set","string","OZS_O_NOV14");
		setup.addVariable("vol","vol to set the flat skew","double","15");
	}

	/**** BEGIN ****/
	public void begin(IContainer container){

		super.begin(container);

		insts=getStringVar("instruments");
		instMonth=getStringVar("instMonth");
		inputVol=getDoubleVar("vol")/100;
		container.filterMarketMessages(insts);

		//ProductData pd = new ProductData("testprod");
		volGrid = container.addGrid("Vols", new String[] {"Strike", "Vol", "Implied"});

		// Gets a collection of instruments configured for this job
		//Collection<String> iids = instruments().getInstrumentIds(insts);

		// Build assigned volatility curve - this looks more complex, but really it's not too bad
		// Basically figuring out strikes and corresponding vols and then setting those for the curve
		/*for (String iid : iids){
			InstrumentDetails det = instruments().getInstrumentDetails(iid);
			if (det.type==InstrumentDetails.Type.CALL){
				strikeSet.add(det.strikePrice);
			}
		}

		//displaySortedSet(strikeSet);

		strikesObj=strikeSet.toArray(new Double[strikeSet.size()]);
		strikes=new double[strikeSet.size()];
		vols=new double[strikes.length];
		Iterator<Double> it = strikeSet.iterator();
		for(int i = 0; i<strikeSet.size(); i++){
			strikes[i]=it.next();
			vols[i]=inputVol+((double)i)/100d;
		}*/		

		/*strikes=getStrikes(iids);
		vols=new double[strikes.length];
		for(int i = 0; i<strikes.length; i++){
			vols[i]=inputVol+((double)i)/100d;
		}*/

		//VolatilityCurve myVols = theos().getVolatilityCurve("ZS_O_NOV14");
		Map<Double,Double> myVols = getVolCurve("OZS_O_NOV14");
		
		//updateVolCurve(instMonth, strikes, vols);
		
		//Map<Double,Double> strikeVols = getVolCurve(instMonth);

		container.stopJob("Done");
	}

	double[] getStrikes(Collection<String> iids) {
		SortedSet<Double> strikeSet=new TreeSet<Double>();
		Double[] strikesObj;
		double[] resultStrikes;

		for (String iid : iids){
			InstrumentDetails det = instruments().getInstrumentDetails(iid);
			if (det.type==InstrumentDetails.Type.CALL){
				strikeSet.add(det.strikePrice);
			}
		}

		strikesObj=strikeSet.toArray(new Double[strikeSet.size()]);
		resultStrikes=new double[strikeSet.size()];
		Iterator<Double> it = strikeSet.iterator();
		for(int i = 0; i<strikeSet.size(); i++){
			resultStrikes[i]=it.next();
		}		

		return resultStrikes;
	}

	Map<Double,Double> getImpliedVolCurve(String monthId) {
		Map<Double,Double> result = new HashMap<Double,Double>();

		instruments().getInstrumentIds(monthId);

		return result;
	}

	Map<Double,Double> getVolCurve(String monthId) {
		double[] resultStrikes;
		double[] resultVols;
		Map<Double,Double> result = new HashMap<Double,Double>();

		VolatilityCurve.Assigned vc = (VolatilityCurve.Assigned)theos().getVolatilityCurve(monthId);
		resultStrikes = vc.strike;
		resultVols = vc.volatility;

		for (int i=0; i<resultStrikes.length; ++i) {
			result.put(Double.valueOf(resultStrikes[i]), Double.valueOf(resultVols[i])); 
		}

		return result;
	}

	void setGridValues(double[] strikes, double[] vols) {
		for (int i=0; i<strikes.length; i++) {
			log("strike: " + strikes[i] + " vol: " + vols[i]);
			volGrid.set(Double.toString(strikes[i]), "Strike", Double.toString(strikes[i]));
			volGrid.set(Double.toString(strikes[i]), "Vol", Double.toString(vols[i]));
		}		
	}
	
	void updateVolCurve(String monthId, double[] strikes, double[] vols) {
		VolatilityCurve.Assigned vc = new VolatilityCurve.Assigned();
		for (int i=0; i<strikes.length; i++) {
			log("strike: " + strikes[i] + " vol: " + vols[i]);
			volGrid.set(Double.toString(strikes[i]), "Strike", Double.toString(strikes[i]));
			volGrid.set(Double.toString(strikes[i]), "Vol", Double.toString(vols[i]));
		}
		vc.strike=strikes;
		vc.volatility=vols;

		// Set curve and publish it
		theos().setVolatilityCurve(monthId,vc);
		theos().publish();		
	}

	void displaySortedSet(SortedSet<Double> set) {
		log(String.format("SET SIZE: %1$d", set.size()));
		for (Double dbl : set) {
			log(String.format("%1$.2f", dbl));
		}
	}

} // class AbstractJob
