import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.InstrumentDetails;
import com.optionscity.freeway.api.VolatilityCurve;


/*
   Job that basically goes through all months for all configured instruments and sets a flat skew for each instrument
*/
public class SetFlatSkew extends AbstractJob {
	String insts, instMonth;
	double inputVol;
	SortedSet<Double> strikeSet=new TreeSet<Double>();

	double[] vols;
	double[] strikes;
	Double[] strikesObj;
	
	public void install(IJobSetup setup){
		setup.setDefaultDescription("Sets a flat skew at a given level");
		setup.addVariable("instruments","instruments to filter on","instruments","ES;O;;;;;");
		setup.addVariable("instMonth","month string to set","string","ES_O_20130621");
		setup.addVariable("vol","vol to set the flat skew","double","15");
	}

	public void begin(IContainer container){

		super.begin(container);

		insts=getStringVar("instruments");
		instMonth=getStringVar("instMonth");
		inputVol=getDoubleVar("vol")/100;
		container.filterMarketMessages(insts);

		// Gets a collection og instruments configured for this job
		Collection<String> iids = instruments().getInstrumentIds(insts);

		// Build assigned volatility curve - this looks more complex, but really it's not too bad
		// Basically figuring out strikes and corresponding vols and then setting those for the curve
		for (String iid : iids){
			InstrumentDetails det = instruments().getInstrumentDetails(iid);
			if (det.type==InstrumentDetails.Type.CALL){
				strikeSet.add(det.strikePrice);
			}
		}
		strikesObj=strikeSet.toArray(new Double[strikeSet.size()]);
		strikes=new double[strikeSet.size()];
		vols=new double[strikes.length];
		Iterator<Double> it = strikeSet.iterator();
		for(int i = 0; i<strikeSet.size(); i++){
			strikes[i]=it.next();
			vols[i]=inputVol; //+((double)i)/100d;
		}
		VolatilityCurve.Assigned vc = new VolatilityCurve.Assigned();
		for (int i=0; i<strikes.length; i++)
			log("strike: " + strikes[i] + " vol: " + vols[i]);
		vc.strike=strikes;
		vc.volatility=vols;

		// Set curve and publish it
		theos().setVolatilityCurve(instMonth,vc);
		theos().publish();

		
		container.stopJob("Done");
	}

}
