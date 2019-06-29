import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import java.sql.Timestamp;
import java.sql.Time;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.optionscity.freeway.api.AbstractJob;
//import com.optionscity.freeway.api.Book;
import com.optionscity.freeway.api.Greeks;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IGrid;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.InstrumentDetails;
//import com.optionscity.freeway.api.InstrumentDetails;
//import com.optionscity.freeway.api.InstrumentDetails.LegDetails;
import com.optionscity.freeway.api.Prices;
import com.optionscity.freeway.api.VolatilityCurve;
import com.optionscity.freeway.api.Position;
import com.optionscity.freeway.api.PositionRisk;
//import com.optionscity.freeway.api.VolatilityCurve;
import com.optionscity.freeway.api.messages.*;
import com.optionscity.freeway.api.services.IInstrumentService;
import com.optionscity.freeway.api.services.ITheoService;
import com.optionscity.freeway.api.services.IPositionService;
//import com.optionscity.freeway.api.Instrument;

public class ATMVolatility extends AbstractJob {

	/*class OptionGreeks {
		double delta;
		double underlyingPrice;
		Time timestamp;

		public OptionGreeks() {

		}
	}*/


	// ****************************************************************************************************************
	// * Option class 
	// ****************************************************************************************************************
	class Option {
		String instrumentId;
		InstrumentDetails details;
		//OptionGreeks greeks;
		double modelVolatility;
		Greeks greeks;
		Greeks bidGreeks;
		Greeks askGreeks;
		//Position position;
		PositionRisk instrumentRisk;

		public Option(String id) {
			instrumentId = id;
			details = instrumentService.getInstrumentDetails(id);
			//greeks = new OptionGreeks();
		}

		public boolean isCall() {
			return instrumentId.endsWith("C");
		}

		public boolean isPut() {
			return instrumentId.endsWith("P");
		}

		public String getId() {
			return instrumentId;
		}

		public InstrumentDetails getDetails() {
			return details;
		}

		public Greeks getGreeks() {
			return greeks;
		}

		public Greeks getBidGreeks() {
			return bidGreeks;
		}

		public Greeks getAskGreeks() {
			return askGreeks;
		}

		public void setGreeks(Greeks greeks) {
			this.greeks = greeks;
		}

		public void setBidGreeks(Greeks greeks) {
			bidGreeks = greeks;  //(Greeks) greeks.clone();
			//updateImpliedVols();
		}

		public void setAskGreeks(Greeks greeks) {
			askGreeks = greeks;  //(Greeks) greeks.clone();
			//updateImpliedVols();
		}		

		public void updateGreeks() {
			greeks = theoService.getGreeks(instrumentId);
			bidGreeks = theoService.getBidGreeks(instrumentId);
			askGreeks = theoService.getAskGreeks(instrumentId);
		}

		public PositionRisk getInstrumentRisk() {
			return instrumentRisk;
		}

		public void updateInstrumentRisk() {
			instrumentRisk = positionService.getInstrumentRisk(instrumentId);
		}

	}

	// ****************************************************************************************************************
	// * OptionStrike class 
	// ****************************************************************************************************************
	class OptionStrike {
		String strike;
		Option call;
		Option put;
		double modelVolatility;
		double impliedVol;
		double bidImpliedVol;
		double askImpliedVol;


		public OptionStrike(String strike) {
			this.strike = strike;			
		}

		public void setCall(String id) {
			call = new Option(id);
		}

		public void setPut(String id) {
			put = new Option(id);
		}

		public Option getCall() {
			return call;
		}

		public Option getPut() {
			return put;
		}

		public String getStrike() {
			return strike;
		}

		public double getStrikeValue() {
			return Double.parseDouble(strike);
		}

		// Use the CALL or PUT for calculations depending on the delta (use PUT for downside, CALL for upside)
		Option CallOrPut(double deltaCutOff) {
			if (call == null || put == null || call.getGreeks() == null) {
				log("CallOrPut() call=" + call + " put=" + put);
				log("CallOrPut() call.getGreeks()=" + call.getGreeks());				
			}

			Option result = null;
			if (call.getGreeks().delta >= deltaCutOff) {
				result = call;
			}
			else {
				result = put;
			}
			return result;
		}
		
		// Same as above except with default of .50 for delta cutoff
		Option CallOrPut() {
			return CallOrPut(.50);
		}

		public void calculateImpliedVols() {
			String id = CallOrPut().getId();
			Prices price = instrumentService.getAllPrices(id);
			double optionMidPrice = (price.bid + price.ask) / 2.0;
			double underlyingMidPrice = (price.underlying_bid + price.underlying_ask) / 2.0;
			double vol = theoService.calculateImpliedVolatility(id, optionMidPrice, underlyingMidPrice);
			double bidVol = theoService.calculateImpliedVolatility(id, price.bid, underlyingMidPrice);
			double askVol = theoService.calculateImpliedVolatility(id, price.ask, underlyingMidPrice);
			impliedVol = round(vol * 100.0);
			bidImpliedVol = round(bidVol * 100.0);			
			askImpliedVol = round(askVol * 100.0);			
		}

		public double getImpliedVol() {
			return impliedVol;
		}

		public double getBidImpliedVol() {
			return bidImpliedVol;
		}

		public double getAskImpliedVol() {
			return askImpliedVol;
		}

		public void setImpliedVol(double vol) {
			impliedVol = vol;
		}

		public void setBidImpliedVol(double vol) {
			bidImpliedVol = vol;
		}

		public void setAskImpliedVol(double vol) {
			askImpliedVol = vol;
		}

		public double getModelVolatility() {
			return modelVolatility;
		}

		public void setModelVolatility(double vol) {
			modelVolatility = vol;
		}

		public void calculateModelVolatility() {
			String id = CallOrPut().getId();
			Prices price = instrumentService.getAllPrices(id);
			double bidVol = theoService.calculateImpliedVolatility(id, price.theo_bid, price.underlying_bid);
			double askVol = theoService.calculateImpliedVolatility(id, price.theo_ask, price.underlying_ask);
			modelVolatility = (bidVol + askVol) / 2.0 * 100.0;
			modelVolatility = round(modelVolatility);
			//log("MODEL VOLATILITY:" + id + " " + String.valueOf(modelVolatility));
		}

		public double distanceFrom50Delta() {
			return Math.abs(.50 - call.getGreeks().delta);
		}
	}

	// ****************************************************************************************************************
	// * OptionMonth class 
	// ****************************************************************************************************************
	class OptionMonth implements Comparable<OptionMonth> {
		String productSymbol;
		String expirationDate;
		InstrumentDetails details;
		Map<String, OptionStrike> strikes;
		SortedSet<String> strikeKeys;
		List<OptionStrike> atmVolStrikes; 
		FuturesContract underlying;		
		String displayText;
		double atmImpliedVol;
		double atmBidImpliedVol;
		double atmAskImpliedVol;
		double atmModelVolatility;

		public OptionMonth(String productSymbol, String expirationDate, InstrumentDetails details) {
			DEBUG_MSG("begin OptionMonth() constructor");
			this.expirationDate = expirationDate;
			this.productSymbol = productSymbol;
			this.details = details;
			//displayText = productSymbol + " " + getSimpleDisplayExpiration(expirationDate);
			displayText = details.symbol + " " + details.displayExpiration;
			DEBUG_MSG("retrieving underlying futures contract...");
			underlying = getFuturesContract(details.underlyingId);
			DEBUG_MSG("underlying retrieved - creating Maps and Lists...");
			strikes = new HashMap<String, OptionStrike>();
			strikeKeys = new TreeSet<String>();
			atmVolStrikes = new ArrayList<OptionStrike>();
		}

		public void addOption(String id) {
			String strikeKey = getOptionStrike(id);
			//log("strike:" + strike);
			if (!strikes.containsKey(strikeKey)) {
				strikes.put(strikeKey, new OptionStrike(strikeKey));
				strikeKeys.add(strikeKey);
			}

			if (isCall(id)) {
				strikes.get(strikeKey).setCall(id);
			}
			else if (isPut(id)) {
				strikes.get(strikeKey).setPut(id);
			}
			else {
				log("SHOULD NEVER GET HERE!");
			}
		}

		public String getDisplayText() {
			return displayText;
		}

		public String getExpirationDate() {
			return expirationDate;
		}

		public boolean hasUnderlying() {
			return underlying != null;
		}
		public FuturesContract getUnderlying() {
			return underlying;
		}

		public List<String> getStrikeKeys() {
			return new ArrayList<String>(strikeKeys);
		}

		public Map<String, OptionStrike> getStrikes() {
			return strikes;
		}

		public int getStrikeCount() {
			return strikes.size();
		}

		public OptionStrike getStrike(String instrumentId) {
			String strike = getOptionStrike(instrumentId);
			return strikes.get(strike);
		}

		public double getATMImpliedVol() {
			return atmImpliedVol;
		}

		public double getATMBidImpliedVol() {
			return atmBidImpliedVol;
		}

		public double getATMAskImpliedVol() {
			return atmAskImpliedVol;
		}

		public double getATMModelVolatility() {
			return atmModelVolatility;
		}

		List<String> getIds() {
			List<String> result = new ArrayList<String>();
			for (OptionStrike strike : strikes.values()) {
				//log("$$$ " + strike.getStrike());
				Option call = strike.getCall();
				Option put = strike.getPut();
				if (call != null)
					result.add(call.getId());
				else
					log("$$$Call is NULL  exp:" + expirationDate + "  strike:" + strike.getStrike());
				if (put != null)
					result.add(put.getId());
				else
					log("$$$Put is NULL   exp:" + expirationDate + "  strike:" + strike.getStrike());
			}
			return result;
		}

		List<String> getCallIds() {
			List<String> result = new ArrayList<String>();
			for (OptionStrike strike : strikes.values()) {
				result.add(strike.getCall().getId());
			}
			return result;
		}

		List<String> getPutIds() {
			List<String> result = new ArrayList<String>();
			for (OptionStrike strike : strikes.values()) {
				result.add(strike.getPut().getId());
			}
			return result;
		}

		// Retrieve the [count] closest strikes to ATM
		public List<OptionStrike> getATMStrikes(int count) {
			Map<Double,OptionStrike> atmStrikes = new HashMap<Double,OptionStrike>();
			updateGreeksForAllStrikes();

			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				if (atmStrikes.size() < count) {
					atmStrikes.put(strike.distanceFrom50Delta(), strike);
				}
				else {
					Double maxDistance = 0.0;
					for (Double distance : atmStrikes.keySet()) {
						if (distance > maxDistance) {
							maxDistance = distance;
						}
					}
					if (strike.distanceFrom50Delta() < maxDistance) {
						atmStrikes.remove(maxDistance);
						atmStrikes.put(strike.distanceFrom50Delta(), strike);
					}
				}
			}	
			return new ArrayList<OptionStrike>(atmStrikes.values());
		}

		public void calculateAverageATMVolatilities(int count) {
			atmVolStrikes = getATMStrikes(count);
			//log("ATM strike count: " + String.valueOf(atmVolStrikes.size()));

			double averageVol = 0.0;
			double averageBidVol = 0.0;
			double averageAskVol = 0.0;
			for (OptionStrike atmStrike : atmVolStrikes) {
				averageVol += atmStrike.getImpliedVol();
				averageBidVol += atmStrike.getBidImpliedVol();
				averageAskVol += atmStrike.getAskImpliedVol();
			}
			atmImpliedVol = round(averageVol / count);
			atmBidImpliedVol = round(averageBidVol / count);
			atmAskImpliedVol = round(averageAskVol / count);
		}

		public void updateGreeksForAllStrikes() {
			//log("updateGreeks " + expirationDate + ": strikeCount=" + String.valueOf(strikes.size()));
			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				strike.getCall().updateGreeks();
				strike.getPut().updateGreeks();
			}						
		}

		public void updatePositionRiskForAllStrikes() {
			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				strike.getCall().updateInstrumentRisk();
				strike.getPut().updateInstrumentRisk();
			}	
			// AND (IF the undelrying is not null) update the risk for this OptionMonth's underlying futures contract...
			if (underlying != null) {
				underlying.updateInstrumentRisk();
			}
		}

		public void calculateModelVolsForAllStrikes() {
			//log("calculateModelVols " + expirationDate + ": strikeCount=" + String.valueOf(strikes.size()));
			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				strike.calculateModelVolatility();
			}			
			updateATMModelVolatility();
		}

		void updateATMModelVolatility() {
			double calcResult = 0.0;
			for (OptionStrike atmStrike : atmVolStrikes) {
				calcResult += atmStrike.getModelVolatility();
			}
			atmModelVolatility = round(calcResult / atmVolStrikes.size());
		}

		public void calculateImpliedVolsForAllStrikes() {
			//log("calculateImpliedVols " + expirationDate + ": strikeCount=" + String.valueOf(strikes.size()));
			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				strike.calculateImpliedVols();
			}						
		}

		public ConsolidatedRisk getConsolidatedRisk() {
			ConsolidatedRisk result = new ConsolidatedRisk();
			for (Map.Entry<String, OptionStrike> entry : strikes.entrySet()) {
				OptionStrike strike = entry.getValue();
				//result.add(strike.call.getGreeks());
				//result.add(strike.put.getGreeks());
				result.add(strike.call.getInstrumentRisk());
				result.add(strike.put.getInstrumentRisk());
			}
			return result;
		}

		@Override
		public int compareTo(final OptionMonth om) {
			return expirationDate.compareTo(om.expirationDate);
		}
	}

	// ****************************************************************************************************************
	// * Product class 
	// ****************************************************************************************************************
	class Product {
		String productSymbol;
		Map<String, OptionMonth> months;
		SortedSet<String> expirationMonths;
		SortedSet<String> expirationDates;
		SortedSet<FuturesContract> underlyingContracts;
		TimeRanges optionsClosedRanges;
		String underlyingSymbol;

		public Product(String symbol, TimeRanges closedRanges, String underlyingSymbol) {
			productSymbol = symbol;
			optionsClosedRanges = closedRanges;
			this.underlyingSymbol = underlyingSymbol;
			months = new HashMap<String, OptionMonth>();
			expirationMonths = new TreeSet<String>();
			expirationDates = new TreeSet<String>();
			underlyingContracts = new TreeSet<FuturesContract>();

			Collection<String> ids = instrumentService.getInstrumentIds(symbol);
			//log("id count = " + String.valueOf(ids.size()));

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");	//("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			String today = dateFormat.format(date); 

			for (String id : ids) {
				DEBUG_MSG("ID:" + id);
				InstrumentDetails details = instrumentService.getInstrumentDetails(id);
				/*if (details.underlyingId == null) {
					DEBUG_MSG("encountered null underlyingId - skipping instrument with id " + id);
					break;
				}*/
				DEBUG_MSG("checking if instrument is an Option...");
				if (isOption(id)) {
					DEBUG_MSG("retrieving option symbol, expiration_date, expiration_month...");
					String optionSymbol = getOptionSymbolMonth(id);
					String expirationDate = getExpirationDate(id);
					String expirationMonth = getExpirationMonth(id);
					DEBUG_MSG("symbol: " + optionSymbol + "    exp: " + expirationMonth);
					
					DEBUG_MSG(today + " <= " + expirationDate);
					if (today.compareTo(expirationDate) <= 0) {
						DEBUG_MSG("expiration date is valid");
						expirationMonths.add(expirationMonth);
						expirationDates.add(expirationDate);

						DEBUG_MSG("check if this month has already been stored...");
						if (!months.containsKey(expirationMonth)) {
							DEBUG_MSG("creating OptionMonth object...");
							OptionMonth optionMonth = new OptionMonth(productSymbol, expirationDate, details);
							DEBUG_MSG("OptionMonth object created");
							months.put(expirationMonth, optionMonth);
							expirationMonths.add(expirationMonth);
							if (optionMonth.getUnderlying() != null) {
								DEBUG_MSG("adding to underlying contracts...");
								underlyingContracts.add(optionMonth.getUnderlying());
							}
							log("** ADDING month KEY for " + productSymbol + ": " + expirationMonth + "   [" + id + " : " + details.type + " : " + details.hasUnderlying() + " : " + details.underlyingId + " : " + details.exchange + " : " + details.displaySymbol + " : " + details.displayExpiration + "]");
						}

						months.get(expirationMonth).addOption(id);
						

						//if (!options.containsKey(optionSymbol))
						//	options.put(optionSymbol, new ArrayList<String>());
					}
				}
			}		

			
			log(productSymbol + "   " + String.valueOf(monthCount()) + " months    " + String.valueOf(underlyingCount()) + " underlyings");
			for (Map.Entry<String, OptionMonth> entry : getMonths().entrySet()) {
				OptionMonth month = entry.getValue();
				log("   MONTH: " + month.getDisplayText() + "  #strikes:" + String.valueOf(month.getStrikeCount()));
			}
			for (FuturesContract underlying : getUnderlyingContracts()) {
				log("### " + underlying.getInstrumentId());
			}
		}

		public String getDisplayText() {
			return productSymbol;
		}

		// Retrieve a month using year and month in format "YYYYMM"
		public OptionMonth getMonth(String monthId) {
			return months.get(monthId);
		}

		public OptionMonth getMonth(int monthIndex) {
			OptionMonth result = null;
			if (monthIndex < monthCount()) {
				String monthId = getMonthIds().get(monthIndex);
				result = getMonth(monthId);
			}
			else {
				log("tried to access month index out of bounds: " + productSymbol + "[" + String.valueOf(monthIndex) + "]");
			}
			return result;
		}

		public Map<String, OptionMonth> getMonths() {
			return months;
		}

		public int monthCount() {
			return months.size();
		}

		public List<String> getMonthIds() {
			return new ArrayList<String>(expirationMonths);
		}

		public List<String> getExpirationDates() {
			return new ArrayList<String>(expirationDates);
		}

		public int getExpirationMonthIndex(String expirationMonth) {
			return getMonthIds().indexOf(expirationMonth);
		}

		public int getExpirationDateIndex(String expirationDate) {
			return getExpirationDates().indexOf(expirationDate);
		}

		public int underlyingCount() {
			return underlyingContracts.size();
		}

		public List<FuturesContract> getUnderlyingContracts() {
			return new ArrayList<FuturesContract>(underlyingContracts);
		}

		public List<OptionMonth> getOptionMonthsWithSpecifiedUnderlying(FuturesContract futuresContract) {
			SortedSet<OptionMonth> sortedMonths = new TreeSet<OptionMonth>();
			for (Map.Entry<String, OptionMonth> entry : months.entrySet()) {
				OptionMonth month = entry.getValue();
				if (month.hasUnderlying()) {
					//DEBUG_MSG("^^ TESTING: " + month.getUnderlying().getInstrumentId() + " == " + futuresContract.getInstrumentId());
					if (month.getUnderlying().equals(futuresContract)) {
						//DEBUG_MSG("^^ MONTH: " + month.getDisplayText() + "  #strikes:" + String.valueOf(month.getStrikeCount()));
						sortedMonths.add(month);
					}
				}
			}
			//DEBUG_MSG("^^ sortedMonths COUNT = " + sortedMonths.size());
			return new ArrayList<OptionMonth>(sortedMonths);
		}

		public OptionStrike getStrike(String instrumentId) {
			String expirationMonth = getExpirationMonth(instrumentId);
			return getMonth(expirationMonth).getStrike(instrumentId);
		}

		public String getSymbol() {
			return productSymbol;
		}

		/*public ATMVols getATM() {
			return atmVols;
		}*/

		public void setImpliedVol(String id, double vol) {
			getStrike(id).setImpliedVol(vol);
		}

		public void setBidImpliedVol(String id, double vol) {
			getStrike(id).setBidImpliedVol(vol);
		}

		public void setAskImpliedVol(String id, double vol) {
			getStrike(id).setAskImpliedVol(vol);
		}

		public void setGreeks(String id, Greeks greeks) {
			if (isCall(id)) {
				getStrike(id).getCall().setGreeks(greeks);
			}
			else if (isPut(id)) {
				getStrike(id).getPut().setGreeks(greeks);
			}
			else {
				log("SHOULD NOT GET HERE!!!");
			}			
		}

		public void setBidGreeks(String id, Greeks greeks) {
			if (isCall(id)) {
				getStrike(id).getCall().setBidGreeks(greeks);
			}
			else if (isPut(id)) {
				getStrike(id).getPut().setBidGreeks(greeks);
			}
			else {
				log("SHOULD NOT GET HERE!!!");
			}			
		}

		public void setAskGreeks(String id, Greeks greeks) {
			if (isCall(id)) {
				getStrike(id).getCall().setAskGreeks(greeks);
			}
			else if (isPut(id)) {
				getStrike(id).getPut().setAskGreeks(greeks);
			}
			else {
				log("SHOULD NOT GET HERE!!!");
			}
		}

		public void calculateModelVolsForMonth(int monthIndex) {
			String monthId = getMonthIds().get(monthIndex);
			calculateModelVolsForMonth(monthId);
		}

		public void calculateModelVolsForMonth(String expirationMonth) {
			months.get(expirationMonth).calculateModelVolsForAllStrikes();
		}

		public void calculateModelVolsForAllMonths() {
			log("calculateModelVolsForAllMonths()");
			for (Map.Entry<String, OptionMonth> entry : months.entrySet()) {
				OptionMonth month = entry.getValue();
				month.calculateModelVolsForAllStrikes();
			}
		}

		public void calculateImpliedVolsForAllMonths() {
			log("calculateImpliedVolsForAllMonths()");
			for (Map.Entry<String,OptionMonth> entry : months.entrySet()) {
				OptionMonth month = entry.getValue();
				month.calculateImpliedVolsForAllStrikes();
			}
		}

		public void updateGreeksForAllMonths() {
			log("updateGreeksForAllMonths()");
			for (Map.Entry<String,OptionMonth> entry : months.entrySet()) {
				OptionMonth month = entry.getValue();
				month.updateGreeksForAllStrikes();
			}
		}

		public void updatePositionRiskForAllMonths() {
			log("updatePositionRiskForAllMonths()");
			for (Map.Entry<String,OptionMonth> entry : months.entrySet()) {
				OptionMonth month = entry.getValue();
				month.updatePositionRiskForAllStrikes();
			}
		}

		public boolean isOptionsMarketOpen() {
			return !optionsClosedRanges.isCurrentTimeWithinAnyRange();
		}

		public String optionsMarketStatus() {
			String result = "OPEN";
			if (isOptionsMarketOpen() == false) {
				result = "CLOSED";
			}
			return result;
		}
	}

	// ****************************************************************************************************************
	// * FuturesContract class 
	// ****************************************************************************************************************
	class FuturesContract implements Comparable<FuturesContract> {
		String instrumentId;
		InstrumentDetails details;
		PositionRisk instrumentRisk;
		String displayText;

		public FuturesContract(String futuresContractId) {
			instrumentId = futuresContractId;
			details = instrumentService.getInstrumentDetails(instrumentId);
			displayText = details.symbol + " " + details.displayExpiration + " Future";			
		}

		public String getDisplayText() {
			return displayText;
		}

		public String getInstrumentId() {
			return instrumentId;
		}

		public InstrumentDetails getDetails() {
			return details;
		}

		public PositionRisk getInstrumentRisk() {
			return instrumentRisk;
		}

		public void updateInstrumentRisk() {
			instrumentRisk = positionService.getInstrumentRisk(instrumentId);
		}		

		public double getDelta() {
			return instrumentRisk.delta;
		}

		public long getDayTradePosition() {
			return instrumentRisk.dayTradePosition;
		}

		public long getCommittedPosition() {
			return instrumentRisk.committedPosition;
		}

		public long getTotalPosition() {
			return instrumentRisk.dayTradePosition + instrumentRisk.committedPosition;
		}

		public double getDayTradePandL() {
			return instrumentRisk.dayTradeProfitAndLoss;
		}

		public double getCommittedPandL() {
			return instrumentRisk.committedProfitAndLoss;
		}

		public double getTotalPandL() {
			return instrumentRisk.dayTradeProfitAndLoss + instrumentRisk.committedProfitAndLoss;
		}

		@Override
		public int compareTo(final FuturesContract fc) {
			return details.expiration.compareTo(fc.details.expiration);
		}
	}

	// ******************************************************************************************************************************
	// *** THIS IS A METHOD IN THE OUTER CLASS THAT WE CAN CALL TO RETRIEVE A FuturesContract object (OR CREATE ONE AND STORE IT) ***
	// ******************************************************************************************************************************
	FuturesContract getFuturesContract(String futuresContractId) {
		DEBUG_MSG("begin getFuturesContract method: futuresContractId = " + futuresContractId);
		FuturesContract result = null;
		if (futuresContractId != null) {
			// if we have already created a futures contract for this futuresContractId, then return that futures contract
			if (futuresContracts.containsKey(futuresContractId)) {
				result = futuresContracts.get(futuresContractId);
			}
			// otherwise, create a new FuturesContract object and return it
			else {
				result = new FuturesContract(futuresContractId);
				futuresContracts.put(futuresContractId, result);
				log("Created new FuturesContract: " + futuresContractId);
			}
		}
		DEBUG_MSG("result = " + String.valueOf(result));
		return result;
	}

	// ****************************************************************************************************************
	// * ConsolidatedRisk class 
	// ****************************************************************************************************************
	class ConsolidatedRisk {
		public double delta = 0.0;
		public double gamma = 0.0;
		public double theta = 0.0;
		public double vega = 0.0;
		public double dayTradeProfitAndLoss = 0.0;
		public double committedProfitAndLoss = 0.0;

		public ConsolidatedRisk() {

		}

		public double getTotalPandL() {
			return dayTradeProfitAndLoss + committedProfitAndLoss;
		}

		public void add(PositionRisk positionRisk) {
			delta += positionRisk.delta;
			gamma += positionRisk.gamma;
			theta += positionRisk.theta;
			vega += positionRisk.vega;
			dayTradeProfitAndLoss += positionRisk.dayTradeProfitAndLoss;
			committedProfitAndLoss += positionRisk.committedProfitAndLoss;
		}
	}

	// ****************************************************************************************************************
	// * TimeRange class 
	// ****************************************************************************************************************
	class TimeRange {
		Date rangeBegin = null;
		Date rangeEnd = null;

		public TimeRange(String startTime, String endTime) {
			DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			try {
				rangeBegin = timeFormat.parse(startTime);
				rangeEnd = timeFormat.parse(endTime);
			}
			catch (java.text.ParseException ex) {
				log("EXCEPTION [TimeRange()]: " + ex);
			}
		}

		public boolean isTimeWithinRange(Date testTime) {
			return (timeIsAfter(testTime, rangeBegin) && timeIsBefore(testTime, rangeEnd));
		}

		boolean timeIsBefore(Date d1, Date d2) {
			DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			return timeFormat.format(d1).compareTo(timeFormat.format(d2)) < 0;
		}

		boolean timeIsAfter(Date d1, Date d2) {
			DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			return timeFormat.format(d1).compareTo(timeFormat.format(d2)) > 0;
		}

	}

	// ****************************************************************************************************************
	// * TimeRanges class 
	// ****************************************************************************************************************
	class TimeRanges {
		List<TimeRange> ranges;

		public TimeRanges() {
			ranges = new ArrayList<TimeRange>();
		}

		public void addRange(TimeRange range) {
			ranges.add(range);
		}

		public boolean isTimeWithinAnyRange(Date testTime) {
			boolean result = false;
			for (TimeRange range : ranges) {
				if (range.isTimeWithinRange(testTime)) {
					result = true;
					break;
				}
			}
			return result;
		}

		public boolean isCurrentTimeWithinAnyRange() {
			Date currentTime = new Date();
			return isTimeWithinAnyRange(currentTime);
		}
	}

	/*class ATMVols {
		final int MONTH_COUNT = 4;
		ATMVol[] vols = new ATMVol[MONTH_COUNT];

		Map<String, ArrayList<String>> options = new HashMap<String, ArrayList<String>>();

		public ATMVols(String symbol) {
			for (int i=0; i<MONTH_COUNT; ++i) {
				vols[i] = new ATMVol(symbol);
			}

			Collection<String> ids = instrumentService.getInstrumentIds(symbol);
			//log("id count = " + String.valueOf(ids.size()));

			for (String id : ids) {
				if (isOption(id)) {
					String optionSymbol = getOptionSymbolMonth(id);
					//log("id: " + optionSymbol);
				
					if (!options.containsKey(optionSymbol))
						options.put(optionSymbol, new ArrayList<String>());

					options.get(optionSymbol).add(id);
				}
			}
		}

		public Map<String, Double> getATMVols(int iMonth) {
			return vols[iMonth].getATMVols();
		}		

		public Map<String, Double> getATMBidVols(int iMonth) {
			return vols[iMonth].getATMBidVols();
		}		

		public Map<String, Double> getATMAskVols(int iMonth) {
			return vols[iMonth].getATMAskVols();
		}		
	}

	class ATMVol {
		String symbol;

		Map<String, Double> atmVols = new HashMap<String, Double>();
		Map<String, Double> atmBidVols = new HashMap<String, Double>();
		Map<String, Double> atmAskVols = new HashMap<String, Double>();
		
		public ATMVol(String symbol) {
			this.symbol = symbol;
		}
	
		public String getSymbol() {
			return symbol;
		}
		
		public Map<String, Double> getATMVols() {
			return atmVols;
		}
		
		public Map<String, Double> getATMBidVols() {
			return atmBidVols;
		}
		
		public Map<String, Double> getATMAskVols() {
			return atmAskVols;
		}
	}*/
	
	// ****************************************************************************************************************
	// * BEGIN MAIN CLASS CODE
	// ****************************************************************************************************************
	static final int ATM_STRIKE_COUNT = 2;			// number of strikes to average when calculating ATM vol
	static final int ATM_VOL_MONTH_COUNT = 4;		// for how many months do we calculate the running ATM vol (to chart)
	static final int BOOK_QTY_THRESHOLD = 10;		// quantity in book must be less than this value to trigger
	static final int PRODUCT_COUNT = 6;				// number of products we are actively trading
	static final int VOL_CHART_UPDATE_COUNT = 2;	// every N times through the timer function, calculate/display the vol charts
	static final int POSITION_UPDATE_COUNT = 2;		// every N times through the timer function, calculate/display the position
	static final int RISK_UPDATE_COUNT = 2;			// every N times through the timer function, calculate/display risk
	static final int PUBLISH_ATM_UPDATE_COUNT = 2;	// every N times through the timer function, publish the average ATM vols

	//String frontMonth = "-201408";
	String frontMonthFutures = "HE-20141014-F;LE-20141031-F;ZL-20141212-F;ZM-20141212-F;ZS-20141114-F;ZW-20141212-F";
	String frontMonthOptions = "HE-20141014;LE-20141005;OZL-20141026;OZM-20141026;OZS-20141026;OZW-20141026";
	String activeOptionsSymbols = "HE-;LE-;OZS-;OZW-;OZM-;OZL-";
	String activeFuturesSymbols = "HE-;LE-;ZS-;ZW-;ZM-;ZL-";
	//Front month futures are HE-October, LE-october, ZL-December, ZM-December, ZS November, ZW-December
	//options - HE-October, LE-October, OZL, October, OZM, October, OZS, October, OZW-October
	
	Map<String, Double> lastPrices = new HashMap<String, Double>();
	
	//Map<String, ATMVols> atmVols = new HashMap<String, ATMVols>();
	Map<String, String> futureSymbols = new HashMap<String, String>();
	//Map<String, List<String>> optionExpirations = new HashMap<String, List<String>>();

	Map<String, Product> products = new HashMap<String, Product>();
	Map<String, FuturesContract> futuresContracts = new HashMap<String, FuturesContract>();

	Map<String, String> riskGridRowSymbolTable = null;

	double low, high, maxgamma, percent;
	String instrumentId;
	
	long updateCount = 0;

	IInstrumentService instrumentService;
	ITheoService theoService;
	IPositionService positionService;

	IGrid futuresGrid;
	IGrid frontMonthFuturesGrid;
	Map<String, IGrid> volGrids = new HashMap<String, IGrid>();
	IGrid totalPositionsGrid;
	IGrid daytradePositionsGrid;
	IGrid riskGrid;

	//@Override
	public void install(IJobSetup setup) {
		//setup.setDefaultDescription("HelloWorld Job Example");
		setup.setDefaultDescription("MCA Trading v1.1");
        /*setup.addVariable("low","low range of delta","double","0");
        setup.addVariable("high","high range of delta","double","0");
        setup.addVariable("maxgamma","maximum gamma before hedge, 0 = no maximum","double","0");
        setup.addVariable("percent","hedge percent 0-100","double","100");
        setup.addVariable("expires","maximum time to wait for order to fill (in ms), 0 = forever","int","0");
        setup.addVariable("instrument","hedging instrument (future)","instruments","");
        
        setup.addProbe("positiondelta","position delta",true);
        setup.addProbe("_last_price", "last price of front-month futures contract", true);
        setup.addProbe("_atm_vol", "current ATM implied volatility", true);
        setup.addProbe("_atm_bid_vol", "current ATM bid implied volatility", true);
        setup.addProbe("_atm_ask_vol", "current ATM ask implied volatility", true);
		*/
	}
	
	public void begin(IContainer container) {		
		super.begin(container);
		
		log("*** STARTING JOB   VERSION: 1.5a ***");
		
		instrumentService = super.instruments();
		theoService = super.theos();
		positionService = super.positions();

		// Grab configured values
		/*
		low = getDoubleVar("low");
		high = getDoubleVar("high");
		expires = getIntVar("expires");
		maxgamma = getDoubleVar("maxgamma");
		percent = getDoubleVar("percent")/100.0;
		*/
		//instrumentId = instruments().getInstrumentId(getStringVar("instrument"));
		//log("using instrument " + instrumentId + " to hedge");
		
		/*getOptionExpirationDates("LE");
		getOptionExpirationDates("HE");
		getOptionExpirationDates("OZS");
		getOptionExpirationDates("OZW");
		getOptionExpirationDates("OZM");
		getOptionExpirationDates("OZL");*/

        container.clearProbes("_atm");
        
		// Create PROBES
		container.addProbe("_last_price",  "last futures price", true);
		container.addProbe("_bid", "futures bid price",  true);
		container.addProbe("_ask", "futures offer price", true);
        container.addProbe("_atm_vol", "current ATM implied volatility", true);
        container.addProbe("_atm_bid_vol", "current ATM bid implied volatility", true);
        container.addProbe("_atm_ask_vol", "current ATM ask implied volatility", true);
        

		TimeRange optionsClosedRange = new TimeRange("13:15:00", "19:00:00");
		TimeRange grainsMorningClosedRange = new TimeRange("07:45:00", "08:30:00");
		TimeRanges optionsClosedRanges = new TimeRanges();
		optionsClosedRanges.addRange(optionsClosedRange);
		TimeRanges grainsClosedRanges = new TimeRanges();
		grainsClosedRanges.addRange(optionsClosedRange);
		grainsClosedRanges.addRange(grainsMorningClosedRange);

		// DEPRECATED: Must discover the futures (FuturesContract) that will serve as the underlyings before we add options with the addOption() method
		//discoverFuturesContracts();
    	
    	DEBUG_MSG("@@@ Adding Products");
    	addProduct("LE", optionsClosedRanges, "LE");
    	addProduct("HE", optionsClosedRanges, "HE");
    	addProduct("OZS", grainsClosedRanges, "ZS");
    	addProduct("OZW", grainsClosedRanges, "ZW");
    	addProduct("OZM", grainsClosedRanges, "ZM");
    	addProduct("OZL", grainsClosedRanges, "ZL");

        /*atmVols.put("LE", new ATMVols("LE"));
        atmVols.put("HE", new ATMVols("HE"));
        atmVols.put("OZS", new ATMVols("OZS"));
        atmVols.put("OZW", new ATMVols("OZW"));
        atmVols.put("OZM", new ATMVols("OZM"));
        atmVols.put("OZL", new ATMVols("OZL"));*/
        
        DEBUG_MSG("@@@ Adding Futures Symbols");
        futureSymbols.put("LE", "LE");
        futureSymbols.put("HE", "HE");
        futureSymbols.put("OZS", "ZS");
        futureSymbols.put("OZW", "ZW");
        futureSymbols.put("OZM", "ZM");
        futureSymbols.put("OZL", "ZL");
        
        // *** SET UP GRIDS ***
		futuresGrid = container.addGrid("Futures", new String[] {"BidQty", "Bid", "Ask", "AskQty"});
		futuresGrid.clear();
		log("To use, create a grid named \"Futures\" with columns \"BidQty\", \"Bid\", \"Ask\" and \"AskQty\"");
        
		frontMonthFuturesGrid = container.addGrid("FrontMonthFutures", new String[] {"Position", "BidQty", "Bid", "Ask", "AskQty", "atmBidVol", "atmAskVol"});
		frontMonthFuturesGrid.clear();
		log("To use, create a grid named \"FrontMonthFutures\" with columns \"Position\", \"BidQty\", \"Bid\", \"Ask\", \"AskQty\", \"atmBidVol\", and \"atmAskVol\"");
        
        totalPositionsGrid = container.addGrid("TotalPositions", new String[] {"ProductSymbol", "Position", "Value"});
        totalPositionsGrid.clear();

		daytradePositionsGrid = container.addGrid("DayTradePositions", new String[] {"ProductSymbol", "Position", "Value"});
		daytradePositionsGrid.clear();

		riskGrid = container.addGrid("Risk", new String[] {"Delta", "Gamma", "Theta", "Vega", "Downside", "ATM", "Upside", "DayTradeProfitLoss", "ProfitLoss", "OptionsDelta", "FuturesDelta", "ExpirationDate"});
		riskGrid.clear();
		log("To use, create a grid named \"Risk\" with columns \"Delta\", \"Gamma\", \"Theta\", \"Vega\", \"Downside\", \"ATM\", \"Upside\", \"DayTradeProfitLoss\", \"ProfitLoss\", \"OptionsDelta\", \"FuturesDelta\", and \"ExpirationDate\"");


		Collection<String> iids = instruments().getInstrumentIds(getStringVar("instruments"));
		
		container.subscribeToMarketBidAskMessages();
		container.subscribeToMarketLastMessages();
		//container.subscribeToTheoMessages();
		
		//VolatilityCurve myVols = theos().getVolatilityCurve("ZS_O_NOV14");
		
		/*Map<Double,Double> myVols = getVolCurve("OZS_O_DEC14");
		log("volcurve size:" + String.valueOf(myVols.size()));*/
		
		/*
		container.subscribeToMarketBidAskMessages();
	    container.subscribeToMarketLastMessages();
	    container.subscribeToOrderMessages();
	    container.subscribeToBookDepthMessages();
	    container.subscribeToRequestForQuoteMessages();
	    container.subscribeToSignals();
	    container.subscribeToTheoMessages();
	    container.subscribeToTradeMessages();
	    container.subscribeToAuctionMessages();
	    container.subscribeToInstrumentMessages();
	 
	    // Filter market data for instruments we know about
	    container.filterMarketMessages(getStringVar("instruments"));

	    // Note: If you're not dynamically adding new symbols, likely via the CX
	    // Exchange or the startSymbol and stopSymbol functions, you do not need
	    // to use filterInstrumentsAsMarketMessage().
	    
	    // Filter market data for new RFQs on newly created instruments
	    container.filterRequestForQuoteAsMarketMessage();
	 
	    // Filter market data for dynamically added symbols
	    container.filterInstrumentsAsMarketMessage();	 
	    */
	}
	//*****************************************************************************************************************	
	
	/*public void discoverFuturesContracts() {
			Collection<String> ids = instrumentService.getInstrumentIds();
			//log("id count = " + String.valueOf(ids.size()));

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");	//("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			String today = dateFormat.format(date); 

			for (String id : ids) {
				//log("ID:" + id);
				InstrumentDetails details = instrumentService.getInstrumentDetails(id);
				if (isOption(id)) {
					String optionSymbol = getOptionSymbolMonth(id);
					String expirationDate = getExpirationDate(id);
					String expirationMonth = getExpirationMonth(id);
					//log("symbol: " + optionSymbol + "    exp: " + expiration);
					
					//log(today + " <= " + expirationDate);
					if (today.compareTo(expirationDate) <= 0) {
						expirationMonths.add(expirationMonth);
						expirationDates.add(expirationDate);

						if (!months.containsKey(expirationMonth)) {
							months.put(expirationMonth, new OptionMonth(productSymbol, expirationDate, details));
							expirationMonths.add(expirationMonth);
							log("** ADDING month KEY for " + productSymbol + ": " + expirationMonth + "   [" + id + " : " + details.type + " : " + details.hasUnderlying() + " : " + details.underlyingId + " : " + details.exchange + " : " + details.displaySymbol + " : " + details.displayExpiration + "]");
						}

						months.get(expirationMonth).addOption(id);

						//if (!options.containsKey(optionSymbol))
						//	options.put(optionSymbol, new ArrayList<String>());
					}
				}
			}
	}*/

	public void addProduct(String productSymbol, TimeRanges closedRanges, String underlyingSymbol) {
		log("adding product: " + productSymbol);

		products.put(productSymbol, new Product(productSymbol, closedRanges, underlyingSymbol));

		addATMVolProbes(productSymbol, true);

		volGrids.put(productSymbol, container.addGrid(productSymbol, new String[] {"Strike", "Vol", "BidImplied", "AskImplied"}));
		volGrids.get(productSymbol).clear();	
	}

	void addATMVolProbes(String symbol, boolean alwaysSendValues) {
		String lcase = symbol.toLowerCase();
		String ucase = symbol.toUpperCase();

		for (int i=0; i<ATM_VOL_MONTH_COUNT; ++i) {
        	String stri = String.valueOf(i);
	        container.addProbe("_atm_vol_" + lcase + "_" + stri, ucase + " current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_" + lcase + "_" + stri, ucase + " current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_" + lcase + "_" + stri, ucase + " current ATM ask implied volatility", true);
	    	container.addProbe("_atm_model_vol_" + lcase + "_" + stri, ucase + " current ATM model volatility", true);
			
			container.getProbe("_atm_vol_" + lcase + "_" + stri).setAlwaysSendValues(alwaysSendValues);
			container.getProbe("_atm_bid_vol_" + lcase + "_" + stri).setAlwaysSendValues(alwaysSendValues);
			container.getProbe("_atm_ask_vol_" + lcase + "_" + stri).setAlwaysSendValues(alwaysSendValues);
			container.getProbe("_atm_model_vol_" + lcase + "_" + stri).setAlwaysSendValues(alwaysSendValues);
		}
	}


	// This event handler will be invoked as quickly as it can consume the events.
	// If the system is unable to process the events as they come in, they will be
	// conflated so that the job will always be receiving the most up-to-date bid
	// or ask event. In other words, the bid/ask updates from teh onMarketBidAsk()
	// method will NOT be queued - you are always assured that the code will receive
	// the most recent bid/ask update.
	
	// The MarketBidAskMessage is an immutable struct that contains the following fields:
	//public final String instrumentId; // Respective Instrument
	//public final long timestamp; // Timestamp of in which event was received	
	public void onMarketBidAsk(MarketBidAskMessage msg) {
		//log("I have received a Bid/Ask event for " + msg.instrumentId);
		Prices prices = instrumentService.getMarketPrices(msg.instrumentId);
		if (isFuture(msg.instrumentId)) { // && isFrontMonth(msg.instrumentId)) {
			//log(String.format("%1$s  MarketBidAsk: %2$d x %3$.3f    %4$.3f x %5$d", msg.instrumentId, prices.bid_size, prices.bid, prices.ask, prices.ask_size));
			container.getProbe("_bid").set(prices.bid);
			container.getProbe("_ask").set(prices.ask);

			// Display the futures market in the grid
			if (isActiveFuturesSymbol(msg.instrumentId)) { 
				futuresGrid.set(msg.instrumentId, "BidQty", prices.bid_size);
				futuresGrid.set(msg.instrumentId, "Bid", prices.bid);
				futuresGrid.set(msg.instrumentId, "Ask",  prices.ask);
				futuresGrid.set(msg.instrumentId, "AskQty", prices.ask_size);	
			}
			
			// If front month, then display the futures market in the front month grid
			if (isFrontMonthFuture(msg.instrumentId)) {
				PositionRisk risk = positionService.getInstrumentRisk(msg.instrumentId);
				frontMonthFuturesGrid.set(msg.instrumentId, "Position", risk.dayTradePosition + risk.committedPosition);

				frontMonthFuturesGrid.set(msg.instrumentId, "BidQty", prices.bid_size);
				frontMonthFuturesGrid.set(msg.instrumentId, "Bid", prices.bid);
				frontMonthFuturesGrid.set(msg.instrumentId, "Ask",  prices.ask);
				frontMonthFuturesGrid.set(msg.instrumentId, "AskQty", prices.ask_size);					
			}
		}
	}
	
	// Unlike bid/ask events, the market last messages are queued (not conflated), so your
	// code will not miss any if it is unable to keep up.
	
	// The MarketLastMessage is an immutable struct that contains the following fields:
	//public final String instrumentId; // Respective Instrument
	//public final long timestamp; // Timestamp of in which event was received
	//public final int quantity; // Quantity of last trade
	//public final double price; // Price of last trade
	public void onMarketLast(MarketLastMessage msg) {
		if (isFrontMonthFuture(msg.instrumentId)) {
			//log(String.format("%1$s  LastPrice: %2$d @ %3$.3f", msg.instrumentId, msg.quantity, msg.price));
			lastPrices.put(msg.instrumentId, msg.price);
			//container.getProbe("_last_price").set(msg.price);
		}
	}
	
	// This event handler is invoked any time a greek or theoretical value has changed for
	// any filtered instruments. Depending on the instruments a job has filtered on, it is 
	// likely that theoretical events could comprise the largest percentage of events over a
	// sampled time period. These theo messages are also conflated.

	/*public void calculateATMVols(String id) { //throws NullPointerException {
		// only process options (not spreads)
		if (isSpread(id)) return;

		// only process options (not futures)
		if (!isOption(id)) return;

		//log("ENTER updateGreeks");

		//if (!isFrontMonthOption(msg.instrumentId)) return;
		//log("onTheo: " + msg.instrumentId);
		//if (getProductSymbol(msg.instrumentId).equals("LE")) {
		//	log(msg.instrumentId + " " + String.valueOf(getOptionExpirationIndex(msg.instrumentId)));
		//}

		// only process if the expiration month is one of the first X months (where X is ATM_VOL_MONTH_COUNT)
		int monthIndex = getOptionExpirationIndex(id);
		
		//if (monthIndex != -1 && monthIndex < ATM_VOL_MONTH_COUNT)
		//	log("***onTheo: monthIndex = " + String.valueOf(monthIndex));
		//else
		//	log("onTheo: monthIndex = " + String.valueOf(monthIndex));

		if (monthIndex != -1 && monthIndex < ATM_VOL_MONTH_COUNT) {
			String symbol = getProductSymbol(id);

			if (symbol != null && products.containsKey(symbol)) {
				//log("***onTheo: monthIndex = " + String.valueOf(monthIndex) + "  symbol = " + symbol);
				//log("***onTheo " + symbol + ": " + msg.instrumentId);
				Greeks greeks = theoService.getGreeks(id);
				selectATMVol(id, greeks, products.get(symbol).getATM().getATMVols(monthIndex));
				
				greeks = theoService.getBidGreeks(id);
				//products.get(symbol).setBidImpliedVol(id, greeks.volatility * 100.0);
				products.get(symbol).setBidGreeks(id, greeks);
				selectATMVol(id, greeks, products.get(symbol).getATM().getATMBidVols(monthIndex));
				
				greeks = theoService.getAskGreeks(id);
				//products.get(symbol).setAskImpliedVol(id, greeks.volatility * 100.0);
				products.get(symbol).setAskGreeks(id, greeks);
				selectATMVol(id, greeks, products.get(symbol).getATM().getATMAskVols(monthIndex));
			}
			else {
				//log("onTheo: monthIndex = " + String.valueOf(monthIndex) + "  symbol = " + symbol);
			}
		}	

		//log("EXIT updateGreeks");	
	}*/

	// The TheoMessage is an immutable struct that contains the following fields:
	//public final String instrumentId; // Respective Instrument
	//public final long timestamp; // Timestamp of in which event was received
	public void onTheo(TheoMessage msg) {
		//log("onTheo: " + msg.instrumentId);

		// A few filters for situations when we want to NOT execute this method

		//updateGreeks(msg.instrumentId);

		// Check the inside bids/offers in the Book
		/*Book book = instrumentService.getBook(msg.instrumentId);	
		if (book.bid.length > 0 && book.bid[0].quantity < BOOK_QTY_THRESHOLD) {
			log(String.format("%1$s Book Inside Bid:  %2$d x %3$.2f", msg.instrumentId, book.bid[0].quantity, book.bid[0].price));
		}
		if (book.ask.length > 0 && book.ask[0].quantity < BOOK_QTY_THRESHOLD) {
			log(String.format("%1$s Book Inside Ask:  %2$.2f x %3$d", msg.instrumentId, book.ask[0].price, book.ask[0].quantity));
		}*/
		
		/*if (greeks.delta > .40 && greeks.delta < .60) {
			log(String.format("Delta: %1$s %2$.2f", msg.instrumentId, greeks.delta));
			//Double ignoreResult = 
			selectATMVol(msg.instrumentId, greeks, atmVols);
		}*/
	}
	
	// The OrderMessage is an immutable struct that contains the following fields:
	//final public long orderId; // Corresponding order ID
	//public final long timestamp; // Timestamp of in which event was received
	//final public Order.Status status;
	// 
	//// With Order.Status being the following enum:
	//public static enum Status {
	//        NEW, BOOKED, FILLED, PARTIAL, CANCELLED, PULLED, EXPIRED, REJECTED;}
	public void onOrder(OrderMessage msg) {
		log("I have received an ORDER event with orderId " + msg.orderId);
	}
	
	// The TradeMessage is an immutable struct that contains the following fields:
	//public final long timestamp; // Timestamp of in which event was received
	//public long tradeId; // The corresponding Trade ID
	//public long orderQuoteId; // The corresponding Order ID
	public void onTrade(TradeMessage msg) {
		log("I have received a TRADE event with tradeId " + msg.tradeId);
	}
	
	// A QuoteMessage contains the following fields:
	// boolean fromExchange
	// String instrumentId
	public void onQuote(QuoteMessage msg) {
		log("I have received a QUOTE event for " + msg.instrumentId);
	}
	
	// Signals are an internal concept and are used as a development tool to notify jobs of
	// custom events. A user can send a Signal via the click of a button, or another job can
	// send a signal. Signals are not conflated, so they are not a recommended way to send
	// realtime trading values in very active instruments because any queuing could result in
	// stale signals arriving at your job.
	
	// The Signal is an extensible object that contains the following default fields:
	//public final String clazz; //
	//public String sender; // Settable descriptor of the sender
	//public String message; // Settable message for the job to utilize	
	public void onSignal(Signal signal) {
		// Handler logic
	}
	
	
	//*****************************************************************************************************************
	public void onTimer() {
		++updateCount;

		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");	//("yyyy/MM/dd HH:mm:ss");
		Date currentTime = new Date();
		String currentTimeText = timeFormat.format(currentTime);
		log("TIMER:" + String.valueOf(updateCount) + "  " + currentTimeText);

		/*for (String id : lastPrices.keySet()) {
			VolatilityCurve vols = theoService.getVolatilityCurve(id);
		}*/
		
		// Format of instrumentMonth is:  parentsymbol_type_expiration
		//String instrumentMonth = "x";
		//VolatilityCurve vols = theoService.getVolatilityCurve(instrumentMonth);

		// TODO: change this looping so we only iterate over the months 

		// *****************************************************************************
		// * First, iterate through each of the option ids to calculate ATM implied vols
		// *****************************************************************************
		/*for (Map.Entry<String, Product> entry : products.entrySet()) {
			String symbol = entry.getKey().toLowerCase();
			log("Product Symbol (calculate ATM vols): " + symbol);
			Product product = entry.getValue();
			
			//log("SYMBOL:" + symbol);

			Map<String, OptionMonth> months = product.getMonths();
			log("MONTHCOUNT:" + String.valueOf(months.size()));
			for (Map.Entry<String, OptionMonth> monthEntry : product.getMonths().entrySet()) {
				String expiration = monthEntry.getKey();
				OptionMonth optionMonth = monthEntry.getValue();
				log("***OptionMonth expiration: " + expiration);
				for (String id : optionMonth.getIds()) {
					//log("calculating ATM vols for " + id); 
					calculateATMVols(id);
				}
			}
		}

		log("***Calculate ATM vols = COMPLETED.");
		*/

		// **********************
		// * Publish the ATM vols
		// **********************
		if ((updateCount % PUBLISH_ATM_UPDATE_COUNT) == 1) {		
			log("***publish ATM vols");
			for (Map.Entry<String, Product> entry : products.entrySet()) {
				String symbol = entry.getKey().toLowerCase();
				Product product = entry.getValue();
				int monthCount = product.monthCount();
				log("Product Symbol (" + product.optionsMarketStatus() + ", publish ATM vols): " + symbol.toUpperCase() + "  [" + String.valueOf(monthCount) + " months]");
				//ATMVols vols = product.getATM();
				
				if (product.isOptionsMarketOpen() == true && monthCount > 0) {
					int atmVolMonthCount = Math.min(ATM_VOL_MONTH_COUNT, monthCount);
					product.updateGreeksForAllMonths();
					// Set the ATM vol probe values
					for (int i=0; i<atmVolMonthCount; ++i) {
						product.getMonth(i).calculateAverageATMVolatilities(ATM_STRIKE_COUNT);

						String idx = "_" + String.valueOf(i);

						// Publish average ATM implied vols
						double vol = product.getMonth(i).getATMImpliedVol();
						double bidVol = product.getMonth(i).getATMBidImpliedVol();
						double askVol = product.getMonth(i).getATMAskImpliedVol();
						container.getProbe("_atm_vol_" + symbol + idx).set(vol);
						container.getProbe("_atm_bid_vol_" + symbol + idx).set(bidVol);
						container.getProbe("_atm_ask_vol_" + symbol + idx).set(askVol);

						// Publish ATM model volatility
						product.calculateModelVolsForMonth(i);
						double modelVol = product.getMonth(i).getATMModelVolatility();
						container.getProbe("_atm_model_vol_" + symbol + idx).set(modelVol);

						log("PROBE (" + symbol + idx + ")  [" + product.getMonth(i).getDisplayText() + "]   bidvol: " + String.valueOf(bidVol) + "   vol: " + String.valueOf(vol) + "   askvol: " + String.valueOf(askVol) + "   modelvol: " + String.valueOf(modelVol));

						if (i == 0) {
							// If a corresponding row exists in the front month futures grid, then display bid/ask vol
							String rowSymbol = getRowSymbol(entry.getKey());
							if (rowSymbol != null) {
								frontMonthFuturesGrid.set(rowSymbol, "atmBidVol", bidVol);
								frontMonthFuturesGrid.set(rowSymbol, "atmAskVol", askVol);
							}
						}
					}
				}
			}
		}

		// ************************************
		// * Update the model volatility charts
		// ************************************
		if ((updateCount % VOL_CHART_UPDATE_COUNT) == 1) {
			log("***model and implied vols");
			int MODEL_VOL_MONTH = 0;
			//VolatilityCurve myVols = theos().getVolatilityCurve("OZS_O_NOV14");
			
			// Iterate products to retrieve the model volatilities
			for (Map.Entry<String, Product> entry : products.entrySet()) {
				Product product = entry.getValue();
				int monthCount = product.monthCount();
				log("Product Symbol (" + product.optionsMarketStatus() + ", model and implied vols): " + product.getSymbol() + "  [" + String.valueOf(monthCount) + " months]");

				if (product.isOptionsMarketOpen() == true && monthCount > 0) {				
					product.calculateModelVolsForAllMonths();
					product.calculateImpliedVolsForAllMonths();
					List<String> monthIds = product.getMonthIds();
					/*for (String monthId : monthIds) {
						log("product:" + product.getSymbol() + "   monthId:" + monthId);
					}*/
					
					// Get the front month options so we can display their model vols
					OptionMonth month = product.getMonth(MODEL_VOL_MONTH);

					if (month != null) {
						Map<String, OptionStrike> strikes = month.getStrikes();
						for (String strikeId : month.getStrikeKeys()) {
							OptionStrike optionStrike = strikes.get(strikeId);
							double strikeValue = optionStrike.getStrikeValue();
							double modelVolatility = optionStrike.getModelVolatility();
							double bidImplied = optionStrike.getBidImpliedVol();
							double askImplied = optionStrike.getAskImpliedVol();
							//Greeks bidGreeks = optionStrike.getCall().getBidGreeks();
							//Greeks askGreeks = optionStrike.getCall().getAskGreeks();
							String symbol = product.getSymbol();
							//log(symbol + " " + strikeId + "   bidimpl:" + roundStr(bidImplied) + " askimpl:" + roundStr(askImplied) + "  modelvol:" + roundStr(modelVolatility));
							volGrids.get(symbol).set(strikeId, "Strike", strikeValue);
							volGrids.get(symbol).set(strikeId, "Vol", modelVolatility);
							volGrids.get(symbol).set(strikeId, "BidImplied", bidImplied);
							volGrids.get(symbol).set(strikeId, "AskImplied", askImplied);
						}
					}
					else {
						log("month is NULL");
					}
				}
			}		
			//calculateModelVols();
			//Map<Double,Double> myVols = getVolCurve("OZS_O_DEC14");
			//log("volcurve size:" + String.valueOf(myVols.size()));
		}	

		// ****************************
		// * Update the position totals
		// ****************************
		if ((updateCount % POSITION_UPDATE_COUNT) == 1) {
			log("***updatePositions");
			updatePositions();
		}

		// This is a KLUDGE to sort the risk grid rows correctly
		if (riskGridRowSymbolTable == null) {
			riskGridRowSymbolTable = new HashMap<String, String>();

			int sortCharAsciiCode = 0;	// starting ASCII char to use for sorting rows alphabetically

			for (Map.Entry<String, Product> entry : products.entrySet()) {
				Product product = entry.getValue();
				for (FuturesContract futuresContract : product.getUnderlyingContracts()) {
					
					List<OptionMonth> monthsWithUnderlying = product.getOptionMonthsWithSpecifiedUnderlying(futuresContract);
					
					// First display the Option Months (that share a common underlying futures contract)
					for (OptionMonth month : monthsWithUnderlying) {
						String rowSymbol = month.getDisplayText();
						String sortChars = getSortChars(sortCharAsciiCode);
						riskGridRowSymbolTable.put(rowSymbol, sortChars + "     " + rowSymbol);
					}

					// THEN display the futures contract
					String rowSymbol = futuresContract.getDisplayText();
					String sortChars = getSortChars(sortCharAsciiCode++);
					riskGridRowSymbolTable.put(rowSymbol, sortChars + rowSymbol);
				}

				// FINALLY display the product symbol by itself (this will serve as the summary row for all months in a product)
				String rowSymbol = product.getDisplayText();
				String sortChars = getSortChars(sortCharAsciiCode++);
				riskGridRowSymbolTable.put(rowSymbol, sortChars + "     *** " + rowSymbol + " ***");
			}			
		}

		// *********************************************
		// * Update the risk (delta, gamma, theta, etc.)
		// *********************************************
		if ((updateCount % RISK_UPDATE_COUNT) == 1) {
			log("***riskUpdate");
			for (Map.Entry<String, Product> entry : products.entrySet()) {
				double productDelta = 0.0;
				double productGamma = 0.0;
				double productTheta = 0.0;
				double productVega = 0.0;
				double productDayTradeProfitAndLoss = 0.0;
				double productTotalProfitAndLoss = 0.0;

				Product product = entry.getValue();
				product.updatePositionRiskForAllMonths();
				for (FuturesContract futuresContract : product.getUnderlyingContracts()) {
					
					double delta = 0.0;
					double gamma = 0.0;
					double theta = 0.0;
					double vega = 0.0;
					double dayTradeProfitAndLoss = 0.0;
					double totalProfitAndLoss = 0.0;

					List<OptionMonth> monthsWithUnderlying = product.getOptionMonthsWithSpecifiedUnderlying(futuresContract);
					
					// First display the Option Months (that share a common underlying futures contract)
					//DEBUG_MSG("$$$$$$ count = " + monthsWithUnderlying.size());
					for (OptionMonth month : monthsWithUnderlying) {
						DEBUG_MSG("$$$$$$ " + month.getDisplayText());

						ConsolidatedRisk risk = month.getConsolidatedRisk();
						String rowSymbol = riskGridRowSymbolTable.get(month.getDisplayText());
						riskGrid.set(rowSymbol, "Delta", round(risk.delta));
						riskGrid.set(rowSymbol, "Gamma", round(risk.gamma));
						riskGrid.set(rowSymbol, "Theta", round(risk.theta));
						riskGrid.set(rowSymbol, "Vega", round(risk.vega));
						riskGrid.set(rowSymbol, "DayTradeProfitLoss", round(risk.dayTradeProfitAndLoss));
						riskGrid.set(rowSymbol, "ProfitLoss", round(risk.getTotalPandL()));

						delta += risk.delta;
						gamma += risk.gamma;
						theta += risk.theta;
						vega += risk.vega;
						dayTradeProfitAndLoss += risk.dayTradeProfitAndLoss;
						totalProfitAndLoss += risk.getTotalPandL();

						// We also need to sum the greeks for ALL MONTHS AND FUTURES of the PRODUCT
						productDelta += risk.delta;
						productGamma += risk.gamma;
						productTheta += risk.theta;
						productVega += risk.vega;
						productDayTradeProfitAndLoss += risk.dayTradeProfitAndLoss;
						productTotalProfitAndLoss += risk.getTotalPandL();
					}

					// THEN display the futures contract
					DEBUG_MSG("$$$ " + futuresContract.getDisplayText());					
					String rowSymbol = riskGridRowSymbolTable.get(futuresContract.getDisplayText());
					delta += futuresContract.getDelta();	// adjust the delta by the futures delta
					dayTradeProfitAndLoss += futuresContract.getDayTradePandL();	// adjust daytrade P&L by the futures daytrade P&L
					totalProfitAndLoss += futuresContract.getTotalPandL();			// adjust total P&L by the futures total P&L
					riskGrid.set(rowSymbol, "Delta", round(delta));
					riskGrid.set(rowSymbol, "Gamma", round(gamma));
					riskGrid.set(rowSymbol, "Theta", round(theta));
					riskGrid.set(rowSymbol, "Vega", round(vega));
					riskGrid.set(rowSymbol, "DayTradeProfitLoss", round(dayTradeProfitAndLoss));
					riskGrid.set(rowSymbol, "ProfitLoss", round(totalProfitAndLoss));					
					
					// We also need to sum the greeks for ALL MONTHS AND FUTURES of the PRODUCT
					productDelta += futuresContract.getDelta();
					productDayTradeProfitAndLoss += futuresContract.getDayTradePandL();
					productTotalProfitAndLoss += futuresContract.getTotalPandL();
				}

				// Display the SUMMARY row for ALL MONTHS AND FUTURES of the PRODUCT
				String rowSymbol = riskGridRowSymbolTable.get(product.getDisplayText());
				riskGrid.set(rowSymbol, "Delta", round(productDelta));
				riskGrid.set(rowSymbol, "Gamma", round(productGamma));
				riskGrid.set(rowSymbol, "Theta", round(productTheta));
				riskGrid.set(rowSymbol, "Vega", round(productVega));
				riskGrid.set(rowSymbol, "DayTradeProfitLoss", round(productDayTradeProfitAndLoss));
				riskGrid.set(rowSymbol, "ProfitLoss", round(productTotalProfitAndLoss));	

				/*for (Map.Entry<String, OptionMonth> monthEntry : product.getMonths().entrySet()) {
					OptionMonth month = monthEntry.getValue();
					ConsolidatedRisk risk = month.getConsolidatedRisk();
					//log(String.valueOf(risk.delta) + "  " + String.valueOf(risk.gamma));
					String rowSymbol = month.getDisplayText();
					riskGrid.set(rowSymbol, "Delta", round(risk.delta));
					riskGrid.set(rowSymbol, "Gamma", round(risk.gamma));
					riskGrid.set(rowSymbol, "Theta", round(risk.theta));
					riskGrid.set(rowSymbol, "Vega", round(risk.vega));
					riskGrid.set(rowSymbol, "DayTradeProfitLoss", round(risk.dayTradeProfitAndLoss));
					riskGrid.set(rowSymbol, "ProfitLoss", round(risk.getTotalPandL()));
					//riskGrid.set(rowSymbol, "Rho", round(risk.vega));
					//riskGrid.set(rowSymbol, "ExpirationDate", month.getExpirationDate());
				}*/
			}

			Collection<Position> positions = positionService.getPositions();

			for (Position position : positions) {
				String id = position.instrumentId;
				//if (id.startsWith("ZS") || id.startsWith("OZS")) {
					PositionRisk risk = positionService.getInstrumentRisk(id);

					//log(id + " dlta=" + String.valueOf(risk.delta));
				//}
			}

		}
	}
	//*****************************************************************************************************************
	
	public void updatePositions() {
		Collection<Position> positions = positionService.getPositions();
		totalPositionsGrid.clear();
		for (Position position : positions) {
			if (isOption(position.instrumentId)) {
				int totalPosition = position.dayTrade + position.committed;
				//log("POSITION " + position.instrumentId + ": " + String.valueOf(position.dayTrade) + " " + String.valueOf(position.committed)); 
				if (totalPosition != 0) {
					totalPositionsGrid.set(position.instrumentId, "ProductSymbol", getProductSymbol(position.instrumentId));
					totalPositionsGrid.set(position.instrumentId, "Position", Math.abs(totalPosition));
					totalPositionsGrid.set(position.instrumentId, "Value", totalPosition);
				}
			}
		}
	}


	/*void calculateModelVols() {
		String matchExpression = "OZS";
		Collection<String> ids = instrumentService.getInstrumentIds(matchExpression);
		log(String.valueOf(ids.size()) + " ids");
		for (String id : ids) {
			if (isOption(id)) {
				Prices price = instrumentService.getAllPrices(id);
				double bidVol = theoService.calculateImpliedVolatility(id, price.theo_bid, price.underlying_bid);
				double askVol = theoService.calculateImpliedVolatility(id, price.theo_ask, price.underlying_ask);
				double modelVol = (bidVol + askVol) / 2;
				log(id + " " + String.valueOf(modelVol));
			}
		}

	}*/

	/*void getOptionExpirationDates(String optionSymbol) {
		TreeSet<String> expirations = new TreeSet<String>();
		Collection<String> instruments = instrumentService.getInstrumentIds(optionSymbol);
		for (String inst : instruments) {
			if (isOption(inst)) {
				expirations.add(getExpirationDate(inst));
			}
		}
		List<String> list = new ArrayList<String>(expirations);
		optionExpirations.put(optionSymbol, list);
		for (String expiry : list) {
			log(optionSymbol + " " + expiry);
		}

		//Map<String, OptionMonth> months = product.getMonths();
		//for (String monthId : months.keySet()) {
		//	log("product:" + product.getSymbol() + "   monthId:" + monthId);
		//}		
	}*/

	// TODO: currently, this method uses ASCII characters starting at zero, so it works up to the SPACE char (32).
	// TODO: to make this more foolproof, we should change it to return TWO characters, then using only 10 or 16
	// 			consecutive non-printable chars, we could handle sorting 100 (10x10) or 256 (16x16) rows (or whatever)
	String getSortChars(int charIndex) {
		return new Character((char) charIndex).toString();
	}

	String getRowSymbol(String optionSymbol) {
		String result = null;
		if (futureSymbols.containsKey(optionSymbol)) {
			String[] rows = frontMonthFuturesGrid.rows();
			String futureSymbol = futureSymbols.get(optionSymbol);
			for (int i=0; i<rows.length; ++i) {
				if (rows[i].startsWith(futureSymbol)) {
					result = rows[i];
					break;
				}
			}
		}
		return result;
	}
	
	// Instrument filtering only looks at the overall characteristics of the
	// instrument. For RFQs, the characteristics of the RFQ as a whole may be
	// different than the characteristics of the legs. For example, for a JUN-SEP
	// spread, the spread itself ceases to exist after JUN expiration, but the legs
	// expire in either JUN or SEP. Filtering in Freeway looks only at the instrument
	// characteristics and does not examine the legs. If you'd like to narrow
	// down the RFQs that you respond to, we suggest checking the instrument IDs
	// of the legs against your filters in your code. In the code snippet below,
	// the system iterates through the legs of a new strategy and makes sure each
	// leg passes the filter on its own.
	/*public void onRequestForQuote(RequestForQuoteMessage m) {
	    InstrumentDetails id = instruments().getInstrumentDetails(m.instrumentId);
	    LegDetails[] lds=id.legs;
	        for (LegDetails ld : lds ){
	            if (! instruments().matches(getStringVar("instruments"),ld.instrumentId)) return;
	        }
	}*/	

	void setProbe(String probeName, double probeValue) {
		container.getProbe(probeName).set(probeValue);
	}

	void setProbe(String probeName, String probeValue) {
		container.getProbe(probeName).set(probeValue);
	}

	double getProbeValue(String probeName) {
		Double dbl = (Double) container.getProbe(probeName).get();
		return dbl.doubleValue();
	}

	String getProbeText(String probeName) {
		return (String) container.getProbe(probeName).get();
	}

	double round(double d, int decimalPlaces) {
		double mult = Math.pow(10.0, decimalPlaces);
		return Math.round(d * mult) / mult;
	}

	double round(double d) {
		return round(d, 2);
	}

	String roundStr(double d) {
		return String.valueOf(round(d));
	}

	boolean isSpread(String id) {
		boolean result = false;
		if (id.contains("|"))
			result = true;	
		return result;
	}
	
	boolean isFuture(String id) {
		boolean result = false;
		if (id.endsWith("-F"))
			result = true;
		return result;
	}
	
	boolean isCall(String id) {
		boolean result = false; 
		if (id.endsWith("C") && isSpread(id) == false)
			result = true;
		return result;
	}
	
	boolean isPut(String id) {
		boolean result = false;
		if (id.endsWith("P") && isSpread(id) == false)
			result = true;
		return result;
	}
	
	boolean isOption(String id) {
		return isCall(id) || isPut(id);
	}
	
	/*boolean isFrontMonth(String id) {
		boolean result = false;
		if (id.contains(frontMonth))
			result = true;
		return result;
	}*/
	
	boolean isFrontMonthFuture(String id) {
		boolean result = false;
		if (isFuture(id) && frontMonthFutures.contains(id))
			result = true;
		return result;
	}

	boolean isFrontMonthOption(String id) {
		boolean result = false;
		String optionSymbol = getOptionSymbolMonth(id);
		if (isOption(id) && frontMonthOptions.contains(optionSymbol))
			result = true;
		return result;
	}
	
	boolean isActiveFuturesSymbol(String id) {
		boolean result = false;
		String symbol = getProductSymbol(id);
		if (activeFuturesSymbols.contains(symbol + "-"))
			result = true;
		return result;
	}

	String getProductSymbol(String id) {
		String result = null;
		int ix = id.indexOf('-');
		if (ix > 0) {
			result = id.substring(0, ix);
		}
		return result;
	}
	
	// Option symbol including product and expiry in format "SS-YYYYMMDD" or "SSS-YYYYMMDD"
	String getOptionSymbolMonth(String id) {
		String result = null;
		int ix1 = id.indexOf('-');
		if (ix1 > 0) {
			int ix2 = id.indexOf('-', ix1+1);
			if (ix2 > 0) {
				result = id.substring(0, ix2);
			}
		}		
		return result;
	}
	
	// Full expiration date in format "YYYYMMDD"
	String getExpirationDate(String id) {
		String result = null;
		int ix1 = id.indexOf('-');
		if (ix1 > 0) {
			int ix2 = id.indexOf('-', ix1+1);
			if (ix2 > 0) {
				result = id.substring(ix1+1, ix2);
			}
		}		
		return result;
	}

	// This returns the expiration month (including year) in format "YYYYMM"
	String getExpirationMonth(String id) {
		String result = null;

		String expiry = getExpirationDate(id);
		if (expiry.length() >= 6) {
			result = expiry.substring(0, 6);
		}
		return result;
	}

	String getOptionStrike(String id) {
		String result = null;
		int ix1 = id.indexOf('-');
		if (ix1 > 0) {
			int ix2 = id.indexOf('-', ix1+1);
			if (ix2 > 0) {
				result = id.substring(ix2+1, id.length()-1);
			}
		}		
		return result;
	}

	Product getProductFromInstrumentId(String id) {
		Product result = products.get(getProductSymbol(id));
		return result;
	}

	int getOptionExpirationIndex(String id) {
		int result = -1;
		Product product = getProductFromInstrumentId(id);
		String expiry = getExpirationDate(id);
		if (product != null && expiry != null) {
			result = product.getExpirationDateIndex(expiry);
		}
		return result;
	}

	String getSimpleDisplayExpiration(String expirationDate) {
		int year = Integer.valueOf(expirationDate.substring(0, 4));
		int month = Integer.valueOf(expirationDate.substring(4, 6));
		int day = Integer.valueOf(expirationDate.substring(6, 8));

		return getMonth3Char(month) + " " + String.valueOf(year);
	}

	String getMonth3Char(int monthIndex) {
		String result = null;
		
		if (monthIndex < 1 || monthIndex > 12) {
			return "ERR";
		}

		switch (monthIndex) {
			case 1: result = "JAN";
				break;
			case 2: result = "FEB";
				break;
			case 3: result = "MAR";
				break;
			case 4: result = "APR";
				break;
			case 5: result = "MAY";
				break;
			case 6: result = "JUN";
				break;
			case 7: result = "JUL";
				break;
			case 8: result = "AUG";
				break;
			case 9: result = "SEP";
				break;
			case 10: result = "OCT";
				break;
			case 11: result = "NOV";
				break;
			case 12: result = "DEC";
				break;
		}

		return result;
	}

	/*Double selectATMVol(String id, Greeks greeks, Map<String, Double> storeVols) {
		if (greeks.delta > .40 && greeks.delta < .60) {
			//log(String.format("Delta: %1$s %2$.2f    %3$.2f", id, greeks.delta, greeks.volatility));
			storeATMVol(id, greeks.volatility, storeVols);
			return greeks.volatility;
		}
		else {
			return null;
		}
	}
	
	void storeATMVol(String id, double vol, Map<String, Double> storeVols) {
		// If our ATMVols don't yet have enough data points, then just add this id/vol
		int count = storeVols.size();
		if (count < ATM_VOL_COUNT) {
			storeVols.put(id,  vol);
			return;
		}
		
		// If this id is already in our ATMVols, then just update it
		if (storeVols.containsKey(id)) {
			storeVols.put(id, vol);
			return;
		}
		
		// OTHERWISE, we will check if this vol is closer to 50 than the ones stored...
		
		// Find the stored vol that is furthest away from 50
		String furthestFromATMId = null;
		double furthestFromATMVol = 0;
		for (String atmVolId : storeVols.keySet()) {
			double atmVol = storeVols.get(atmVolId);
			if (Math.abs(50 - atmVol) > furthestFromATMVol) {
				furthestFromATMVol = atmVol;
				furthestFromATMId = atmVolId;
			}
		}
		
		// If the vol to store is closer to ATM than the existing stored vols...
		if (Math.abs(50 - vol) < Math.abs(50 - furthestFromATMVol)) {
			storeVols.remove(furthestFromATMId);
			storeVols.put(id,  vol);
		}
		
		//log("vol stored*");
		
		// Display each of the stored ATM vols
		//for (String atmVolId : atmVols.keySet()) {
		//	//log(String.format("VOLS: %1$s %1$.2f", atmVolId.toString(), atmVols.get(atmVolId)));
		//	log(atmVolId);
		//}
		
		//log(String.format("ATMVol: %1$.2f", getAverageATMVol()));
	}
	
	double getAverageATMVol(Map<String, Double> storeVols) {
		int count = storeVols.size();
		double average = 0.0;
		for (String atmVolId : storeVols.keySet()) {
			average += storeVols.get(atmVolId);
		}
		average = average / count;
		
		double roundedAverage = Math.round(average * 10000) / 100.0;
		return roundedAverage;
	}*/
	
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

		VolatilityCurve.TwoSided vc = (VolatilityCurve.TwoSided)theos().getVolatilityCurve(monthId);
		/*resultStrikes = vc.strike;
		resultVols = vc.volatility;

		for (int i=0; i<resultStrikes.length; ++i) {
			result.put(Double.valueOf(resultStrikes[i]), Double.valueOf(resultVols[i])); 
		}*/

		return result;
	}

	/*void setGridValues(double[] strikes, double[] vols) {
		for (int i=0; i<strikes.length; i++) {
			log("strike: " + strikes[i] + " vol: " + vols[i]);
			volGrid.set(Double.toString(strikes[i]), "Strike", Double.toString(strikes[i]));
			volGrid.set(Double.toString(strikes[i]), "Vol", Double.toString(vols[i]));
		}		
	}*/
	
	/*void updateVolCurve(String monthId, double[] strikes, double[] vols) {
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
	}*/
	
	void DEBUG_MSG(String msg) {
		//log(msg);
	}
} // class
