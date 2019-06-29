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

//import java.util.Timestamp;
import java.util.Date;
import java.util.Time;

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
//import com.optionscity.freeway.api.VolatilityCurve;
import com.optionscity.freeway.api.messages.*;
import com.optionscity.freeway.api.services.IInstrumentService;
import com.optionscity.freeway.api.services.ITheoService;
//import com.optionscity.freeway.api.Instrument;

public class ATMVolatilityChart2 extends AbstractJob {

	class OptionGreeks {
		double delta;
		double underlyingPrice;
		Time timestamp;
	}

	class Option {
		String optionSymbol;
		OptionGreeks greeks;
		//double strikePrice;
	}

	class OptionStrike {
		int strikePrice;
		Option call;
		Option put;
	}

	class OptionMonth {
		String monthAbbrev;
		Date expirationDate;
		List<OptionStrike> strikes;
	}

	class Product {
		String productSymbol;
		List<OptionMonth> months;
	}

	class ATMVols {
		final int MONTH_COUNT = 4;
		ATMVol[] vols = new ATMVol[MONTH_COUNT];

		Map<String, ArrayList<String>> options = new HashMap<String, ArrayList<String>>();

		public ATMVols(String symbol) {
			for (int i=0; i<MONTH_COUNT; ++i) {
				vols[i] = new ATMVol(symbol);
			}

			Collection<String> ids = instrumentService.getInstrumentIds(symbol);
			log("id count = " + String.valueOf(ids.size()));

			for (String id : ids) {
				if (isOption(id)) {
					String optionSymbol = getOptionSymbol(id);
					log("id: " + optionSymbol);
				
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
	}
	
	static final int ATM_VOL_COUNT = 2;				// number of strikes to average when calculating ATM vol
	static final int ATM_VOL_MONTH_COUNT = 4;		// for how many months do we calculate the running ATM vol (to chart)
	static final int BOOK_QTY_THRESHOLD = 10;		// quantity in book must be less than this value to trigger
	static final int PRODUCT_COUNT = 6;				// number of products we are actively trading

	//String frontMonth = "-201408";
	String frontMonthFutures = "HE-20141014-F;LE-20141031-F;ZL-20141212-F;ZM-20141212-F;ZS-20141114-F;ZW-20141212-F";
	String frontMonthOptions = "HE-20141014;LE-20140905;OZL-20140926;OZM-20140926;OZS-20140926;OZW-20140926";
	String activeOptionsSymbols = "HE-;LE-;OZS-;OZW-;OZM-;OZL-";
	String activeFuturesSymbols = "HE-;LE-;ZS-;ZW-;ZM-;ZL-";
	//Front month futures are HE-October, LE-october, ZL-December, ZM-December, ZS November, ZW-December
	//options - HE-October, LE-October, OZL, October, OZM, October, OZS, October, OZW-October
	
	Map<String, Double> lastPrices = new HashMap<String, Double>();
	
	Map<String, ATMVols> atmVols = new HashMap<String, ATMVols>();
	Map<String, String> futureSymbols = new HashMap<String, String>();
	Map<String, List<String>> optionExpirations = new HashMap<String, List<String>>();

	double low, high, maxgamma, percent;
	int expires;
	String instrumentId;
	
	IInstrumentService instrumentService;
	ITheoService theoService;
	
	IGrid futuresGrid;
	IGrid frontMonthFuturesGrid;
	IGrid volGrid;
	
	//@Override
	public void install(IJobSetup setup) {
		//setup.setDefaultDescription("HelloWorld Job Example");
		setup.setDefaultDescription("MCA Trading v1.0");
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
		
		log("*** STARTING JOB   VERSION: 1.4a ***");
		
		instrumentService = super.instruments();
		theoService = super.theos();
		
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
		getOptionExpirationDates("LE");
		getOptionExpirationDates("HE");
		getOptionExpirationDates("OZS");
		getOptionExpirationDates("OZW");
		getOptionExpirationDates("OZM");
		getOptionExpirationDates("OZL");

        container.clearProbes("_atm");
        
		// Create PROBES
		container.addProbe("_last_price",  "last futures price", true);
		container.addProbe("_bid", "futures bid price",  true);
		container.addProbe("_ask", "futures offer price", true);
        container.addProbe("_atm_vol", "current ATM implied volatility", true);
        container.addProbe("_atm_bid_vol", "current ATM bid implied volatility", true);
        container.addProbe("_atm_ask_vol", "current ATM ask implied volatility", true);
                
        for (int i=0; i<ATM_VOL_MONTH_COUNT; ++i) {
        	String stri = String.valueOf(i);

	        container.addProbe("_atm_vol_le_" + stri, "LE current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_le_" + stri, "LE current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_le_" + stri, "LE current ATM ask implied volatility", true);

	        container.addProbe("_atm_vol_he_" + stri, "HE current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_he_" + stri, "HE current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_he_" + stri, "HE current ATM ask implied volatility", true);
	        
	        container.addProbe("_atm_vol_ozl_" + stri, "ZL current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_ozl_" + stri, "ZL current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_ozl_" + stri, "ZL current ATM ask implied volatility", true);
	        
	        container.addProbe("_atm_vol_ozm_" + stri, "ZM current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_ozm_" + stri, "ZM current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_ozm_" + stri, "ZM current ATM ask implied volatility", true);
	        
	        container.addProbe("_atm_vol_ozs_" + stri, "ZS current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_ozs_" + stri, "ZS current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_ozs_" + stri, "ZS current ATM ask implied volatility", true);
	        
	        container.addProbe("_atm_vol_ozw_" + stri, "ZW current ATM implied volatility", true);
	        container.addProbe("_atm_bid_vol_ozw_" + stri, "ZW current ATM bid implied volatility", true);
	        container.addProbe("_atm_ask_vol_ozw_" + stri, "ZW current ATM ask implied volatility", true);
    	}

        atmVols.put("LE", new ATMVols("LE"));
        atmVols.put("HE", new ATMVols("HE"));
        atmVols.put("OZS", new ATMVols("OZS"));
        atmVols.put("OZW", new ATMVols("OZW"));
        atmVols.put("OZM", new ATMVols("OZM"));
        atmVols.put("OZL", new ATMVols("OZL"));
        
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
        
		frontMonthFuturesGrid = container.addGrid("FrontMonthFutures", new String[] {"BidQty", "Bid", "Ask", "AskQty", "atmBidVol", "atmAskVol"});
		frontMonthFuturesGrid.clear();
		log("To use, create a grid named \"FrontMonthFutures\" with columns \"BidQty\", \"Bid\", \"Ask\", \"AskQty\", \"atmBidVol\" and \"atmAskVol\"");
        
		volGrid = container.addGrid("Vols", new String[] {"Strike", "Vol", "Implied"});
		
		//Collection<String> iids = instruments().getInstrumentIds(getStringVar("instruments"));
		
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
	
	// The TheoMessage is an immutable struct that contains the following fields:
	//public final String instrumentId; // Respective Instrument
	//public final long timestamp; // Timestamp of in which event was received
	public void onTheo(TheoMessage msg) {
		//log("onTheo: " + msg.instrumentId);

		// A few filters for situations when we want to NOT execute this method

		// only process options (not spreads)
		if (isSpread(msg.instrumentId)) return;

		// only process options (not futures)
		if (!isOption(msg.instrumentId)) return;

		//if (!isFrontMonthOption(msg.instrumentId)) return;
		//log("onTheo: " + msg.instrumentId);
		//if (getProductSymbol(msg.instrumentId).equals("LE")) {
		//	log(msg.instrumentId + " " + String.valueOf(getOptionExpirationIndex(msg.instrumentId)));
		//}

		// only process if the expiration month is one of the first X months (where X is ATM_VOL_MONTH_COUNT)
		int monthIndex = getOptionExpirationIndex(msg.instrumentId);
		
		/*if (monthIndex != -1 && monthIndex < ATM_VOL_MONTH_COUNT)
			log("***onTheo: monthIndex = " + String.valueOf(monthIndex));
		else
			log("onTheo: monthIndex = " + String.valueOf(monthIndex));*/

		if (monthIndex != -1 && monthIndex < ATM_VOL_MONTH_COUNT) {
			String symbol = getProductSymbol(msg.instrumentId);

			if (symbol != null && atmVols.containsKey(symbol)) {
				log("***onTheo: monthIndex = " + String.valueOf(monthIndex) + "  symbol = " + symbol);
				//log("***onTheo " + symbol + ": " + msg.instrumentId);
				Greeks greeks = theoService.getGreeks(msg.instrumentId);
				selectATMVol(msg.instrumentId, greeks, atmVols.get(symbol).getATMVols(monthIndex));
				
				greeks = theoService.getBidGreeks(msg.instrumentId);
				selectATMVol(msg.instrumentId, greeks, atmVols.get(symbol).getATMBidVols(monthIndex));
				
				greeks = theoService.getAskGreeks(msg.instrumentId);
				selectATMVol(msg.instrumentId, greeks, atmVols.get(symbol).getATMAskVols(monthIndex));
			}
			else
				log("onTheo: monthIndex = " + String.valueOf(monthIndex) + "  symbol = " + symbol);
		}

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
	
	
	public void onTimer() {
		//log("I have been called on the configured timing interval");
		
		/*for (String id : lastPrices.keySet()) {
			VolatilityCurve vols = theoService.getVolatilityCurve(id);
		}*/
		
		// Format of instrumentMonth is:  parentsymbol_type_expiration
		//String instrumentMonth = "x";
		//VolatilityCurve vols = theoService.getVolatilityCurve(instrumentMonth);
		
		for (Map.Entry<String, ATMVols> entry : atmVols.entrySet()) {
			String symbol = entry.getKey().toLowerCase();
			ATMVols vols = entry.getValue();
			
			// Set the ATM vol probe values
			for (int i=0; i<ATM_VOL_MONTH_COUNT; ++i) {
				String idx = "_" + String.valueOf(i);

				double vol = getAverageATMVol(vols.getATMVols(i));
				double bidVol = getAverageATMVol(vols.getATMBidVols(i));
				double askVol = getAverageATMVol(vols.getATMAskVols(i));
				container.getProbe("_atm_vol_" + symbol + idx).set(vol);
				container.getProbe("_atm_bid_vol_" + symbol + idx).set(bidVol);
				container.getProbe("_atm_ask_vol_" + symbol + idx).set(askVol);

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

	void getOptionExpirationDates(String optionSymbol) {
		TreeSet<String> expirations = new TreeSet<String>();
		Collection<String> instruments = instrumentService.getInstrumentIds(optionSymbol);
		for (String inst : instruments) {
			if (isOption(inst)) {
				expirations.add(getOptionExpiration(inst));
			}
		}
		List<String> list = new ArrayList<String>(expirations);
		optionExpirations.put(optionSymbol, list);
		for (String expiry : list) {
			log(optionSymbol + " " + expiry);
		}
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
		String optionSymbol = getOptionSymbol(id);
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
	
	String getOptionSymbol(String id) {
		String result = null;
		int ix = id.indexOf('-');
		if (ix > 0) {
			ix = id.indexOf('-', ix+1);
			if (ix > 0) {
				result = id.substring(0, ix);
			}
		}		
		return result;
	}
	
	String getOptionExpiration(String id) {
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

	int getOptionExpirationIndex(String id) {
		int result = -1;
		String expiry = getOptionExpiration(id);
		if (expiry != null) {
			String symbol = getProductSymbol(id);
			if (optionExpirations.containsKey(symbol)) {
				result = optionExpirations.get(symbol).indexOf(expiry);
			}
		}
		return result;
	}

	Double selectATMVol(String id, Greeks greeks, Map<String, Double> storeVols) {
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
		
		/*log("vol stored*");
		
		// Display each of the stored ATM vols
		for (String atmVolId : atmVols.keySet()) {
			//log(String.format("VOLS: %1$s %1$.2f", atmVolId.toString(), atmVols.get(atmVolId)));
			log(atmVolId);
		}*/
		
		//log(String.format("ATMVol: %1$.2f", getAverageATMVol()));
	}
	
	double getAverageATMVol(Map<String, Double> storeVols) {
		int count = storeVols.size();
		double average = 0.0;
		for (String atmVolId : storeVols.keySet()) {
			average += storeVols.get(atmVolId);
		}
		average = average / count;
		
		double roundedAverage = Math.round(average * 1000) / 10.0;
		return roundedAverage;
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
	
} // class
