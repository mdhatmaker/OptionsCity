//import java.util.Collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.Book;
import com.optionscity.freeway.api.Greeks;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IGrid;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.Position;
//import com.optionscity.freeway.api.InstrumentDetails;
//import com.optionscity.freeway.api.InstrumentDetails.LegDetails;
import com.optionscity.freeway.api.Prices;
//import com.optionscity.freeway.api.VolatilityCurve;
import com.optionscity.freeway.api.messages.*;
import com.optionscity.freeway.api.services.IInstrumentService;
import com.optionscity.freeway.api.services.IPositionService;
import com.optionscity.freeway.api.services.ITheoService;
//import com.optionscity.freeway.api.Instrument;

public class GridTest extends AbstractJob {

	static final int ATM_VOL_COUNT = 2;
	static final int BOOK_QTY_THRESHOLD = 10;
	
	String frontMonth = "-201411";

	Map<String, Double> lastPrices = new HashMap<String, Double>();
	Map<String, Double> atmVols = new HashMap<String, Double>();
	Map<String, Double> atmBidVols = new HashMap<String, Double>();
	Map<String, Double> atmAskVols = new HashMap<String, Double>();
	Map<String, Position> positions = new HashMap<String, Position>();
	
	double low, high, maxgamma, percent;
	int expires;
	String instrumentId;
	
	IInstrumentService instrumentService;
	ITheoService theoService;
	IPositionService positionService;
	
	IGrid infoGrid;
	double prevPrice;
	long qty=0;
	Map<String, Long> idQty=new HashMap<String,Long>();
	
	IGrid futuresGrid;
	IGrid insideBookGrid;
	
	//@Override
	public void install(IJobSetup setup) {
		//setup.setDefaultDescription("HelloWorld Job Example");
		setup.setDefaultDescription("calculate ATM volatility");
        /*setup.addVariable("low","low range of delta","double","0");
        setup.addVariable("high","high range of delta","double","0");
        setup.addVariable("maxgamma","maximum gamma before hedge, 0 = no maximum","double","0");
        setup.addVariable("percent","hedge percent 0-100","double","100");
        setup.addVariable("expires","maximum time to wait for order to fill (in ms), 0 = forever","int","0");
        setup.addVariable("instrument","hedging instrument (future)","instruments","");
    
        setup.addVariable("instruments", "instruments to filter on", "instruments", "ES;;;;;;");
        
        setup.addProbe("positiondelta","position delta",true);
        setup.addProbe("_last_price", "last price of front-month futures contract", true);
        setup.addProbe("_atm_vol", "current ATM implied volatility", true);
        setup.addProbe("_atm_bid_vol", "current ATM bid implied volatility", true);
        setup.addProbe("_atm_ask_vol", "current ATM ask implied volatility", true);
		*/
	}
	
	public void begin(IContainer container) {		
		super.begin(container);
		
		log("*** VERSION: 1.2 ***");
		
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
		
        //container.clearProbes("_atm");
        
		// Create PROBES
		container.addProbe("_last_price",  "last futures price", true);
		container.addProbe("_bid", "futures bid price",  true);
		container.addProbe("_ask", "futures offer price", true);
        container.addProbe("_atm_vol", "current ATM implied volatility", true);
        container.addProbe("_atm_bid_vol", "current ATM bid implied volatility", true);
        container.addProbe("_atm_ask_vol", "current ATM ask implied volatility", true);


        
		//Collection<String> iids = instruments().getInstrumentIds(getStringVar("instruments"));
		
		container.subscribeToMarketBidAskMessages();
		container.subscribeToMarketLastMessages();
		container.subscribeToTheoMessages();
		container.subscribeToTradeMessages();
		
		infoGrid = container.addGrid("Market Last",  new String[] {"Price", "Volume", "Input1", "Input2"});
		
		log("To use, create a grid named \"Market Last\" with columns \"Price\", \"Volume\" and \"Input\"");
		log("Output is a grid of last price and total volume over the preceding timer interval");
		
		futuresGrid = container.addGrid("Futures", new String[] {"BidQty", "Bid", "Ask", "AskQty"});
		log("To use, create a grid named \"Futures\" with columns \"BidQty\", \"Bid\", \"Ask\" and \"AskQty\"");
		
		insideBookGrid = container.addGrid("Inside Book",  new String[] {"BidQty", "Bid", "Ask", "AskQty"});
		log("To use, create a grid named \"Inside Book\" with columns \"BidQty\", \"Bid\", \"Ask\" and \"AskQty\"");

		
		updatePositions();
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
		if (isFuture(msg.instrumentId)) {
			// If this is the front-month futures contract, then publish the _bid and _ask probes
			if (isFrontMonth(msg.instrumentId)) {
				log(String.format("%1$s  MarketBidAsk: %2$d x %3$.3f    %4$.3f x %5$d", msg.instrumentId, prices.bid_size, prices.bid, prices.ask, prices.ask_size));
				container.getProbe("_bid").set(prices.bid);
				container.getProbe("_ask").set(prices.ask);
			}

			// Display the futures market in the grid
			futuresGrid.set(msg.instrumentId, "BidQty", prices.bid_size);
			futuresGrid.set(msg.instrumentId, "Bid", prices.bid);
			futuresGrid.set(msg.instrumentId, "Ask",  prices.ask);
			futuresGrid.set(msg.instrumentId, "AskQty", prices.ask_size);
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
		if (isFuture(msg.instrumentId) && isFrontMonth(msg.instrumentId)) {
			log(String.format("%1$s  LastPrice: %2$d @ %3$.3f", msg.instrumentId, msg.quantity, msg.price));
			lastPrices.put(msg.instrumentId, msg.price);
			//container.getProbe("_last_price").set(msg.price);
		}

		infoGrid.set(msg.instrumentId, "Price", msg.price);
		long oldVol = idQty.containsKey(msg.instrumentId) ? idQty.get(msg.instrumentId) : 0;
		idQty.put(msg.instrumentId, oldVol+msg.quantity);
	}
	
	// This event handler is invoked any time a greek or theoretical value has changed for
	// any filtered instruments. Depending on the instruments a job has filtered on, it is 
	// likely that theoretical events could comprise the largest percentage of events over a
	// sampled time period. These theo messages are also conflated.
	
	// The TheoMessage is an immutable struct that contains the following fields:
	//public final String instrumentId; // Respective Instrument
	//public final long timestamp; // Timestamp of in which event was received
	public void onTheo(TheoMessage msg) {
		// DO NOT DEAL WITH SPREADS
		if (isSpread(msg.instrumentId)) return;

		// Check the inside bids/offers in the Book
		Book book = instrumentService.getBook(msg.instrumentId);	
		if (book.bid.length > 0 && book.bid[0].quantity < BOOK_QTY_THRESHOLD) {
			//log(String.format("%1$s Book Inside Bid:  %2$d x %3$.2f", msg.instrumentId, book.bid[0].quantity, book.bid[0].price));
			insideBookGrid.set(msg.instrumentId, "BidQty", book.bid[0].quantity);
			insideBookGrid.set(msg.instrumentId, "Bid", book.bid[0].price);
		}
		if (book.ask.length > 0 && book.ask[0].quantity < BOOK_QTY_THRESHOLD) {
			//log(String.format("%1$s Book Inside Ask:  %2$.2f x %3$d", msg.instrumentId, book.ask[0].price, book.ask[0].quantity));
			insideBookGrid.set(msg.instrumentId, "Ask", book.ask[0].price);
			insideBookGrid.set(msg.instrumentId, "AskQty", book.ask[0].quantity);
		}

		// FRONT-MONTH ONLY
		if (!isFrontMonth(msg.instrumentId)) return;

		Greeks greeks = theoService.getGreeks(msg.instrumentId);
		selectATMVol(msg.instrumentId, greeks, atmVols);
		
		greeks = theoService.getBidGreeks(msg.instrumentId);
		selectATMVol(msg.instrumentId, greeks, atmBidVols);
		
		greeks = theoService.getAskGreeks(msg.instrumentId);
		selectATMVol(msg.instrumentId, greeks, atmAskVols);
		
		
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
		updatePositions();
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
		
		container.getProbe("_atm_vol").set(getAverageATMVol(atmVols));
		container.getProbe("_atm_bid_vol").set(getAverageATMVol(atmBidVols));
		container.getProbe("_atm_ask_vol").set(getAverageATMVol(atmAskVols));
		//container.getProbe("_atm_ask_vol").set("28.3");
		
		for (Map.Entry<String, Long> e : idQty.entrySet()) {
			infoGrid.set(e.getKey(), "Volume", e.getValue());
			e.setValue(0L);
		}
		
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
	
	boolean isFrontMonth(String id) {
		boolean result = false;
		if (id.contains(frontMonth))
			result = true;
		return result;
	}
	
	void updatePositions() {
		Collection<Position> getPos = positionService.getPositions();
		log(String.format("Position COUNT: %1$d", getPos.size()));
		for (Position pos : getPos) {
			String id = pos.instrumentId;
			int dayTrade = pos.dayTrade;
			int committed = pos.committed;
			// Store this position in our positions HashMap by the instrumentId
			positions.put(id, pos);
			log(String.format("%1$s  POSITION: %2$d %3$d", id, dayTrade, committed));			
		}
	}
	
	Double selectATMVol(String id, Greeks greeks, Map<String, Double> storeVols) {
		double vol = greeks.volatility;
		if (greeks.delta > .40 && greeks.delta < .60) {
			//log(String.format("Delta: %1$s %2$.2f", id, greeks.delta));
			storeATMVol(id, greeks.delta, storeVols);
			return vol;
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
		
		log(String.format("ATMVol: %1$.2f", getAverageATMVol(storeVols)));
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
	
} // class
