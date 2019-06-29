//import java.util.Collection;

import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.Greeks;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;
//import com.optionscity.freeway.api.InstrumentDetails;
//import com.optionscity.freeway.api.InstrumentDetails.LegDetails;
import com.optionscity.freeway.api.Prices;
import com.optionscity.freeway.api.messages.*;
import com.optionscity.freeway.api.services.IInstrumentService;
import com.optionscity.freeway.api.services.ITheoService;
//import com.optionscity.freeway.api.Instrument;

public class GUITest extends AbstractJob {

	double low, high, maxgamma, percent;
	int expires;
	String instrumentId;
	
	IInstrumentService instrumentService;
	ITheoService theoService;
	
	//@Override
	public void install(IJobSetup setup) {
		//setup.setDefaultDescription("HelloWorld Job Example");
		setup.setDefaultDescription("balance portfolio delta using futures");
        setup.addVariable("low","low range of delta","double","0");
        setup.addVariable("high","high range of delta","double","0");
        setup.addVariable("maxgamma","maximum gamma before hedge, 0 = no maximum","double","0");
        setup.addVariable("percent","hedge percent 0-100","double","100");
        setup.addVariable("expires","maximum time to wait for order to fill (in ms), 0 = forever","int","0");
        setup.addVariable("instrument","hedging instrument (future)","instruments","");
        
        setup.addProbe("positiondelta","position delta",true);
        setup.addProbe("_last_price", "last price of front-month futures contract", true);
	}
	
	public void begin(IContainer container) {		
		super.begin(container);
		
		log("*** VERSION: 1.1 ***");
		
		instrumentService = super.instruments();
		theoService = super.theos();
		
		// Grab configured values
		low = getDoubleVar("low");
		high = getDoubleVar("high");
		expires = getIntVar("expires");
		maxgamma = getDoubleVar("maxgamma");
		percent = getDoubleVar("percent")/100.0;
		//instrumentId = instruments().getInstrumentId(getStringVar("instrument"));
		//log("using instrument " + instrumentId + " to hedge");
		
		// Create PROBES
		container.addProbe("_last_price",  "last futures price", true);
		container.addProbe("_bid", "futures bid price",  true);
		container.addProbe("_ask", "futures offer price", true);
		
		//Collection<String> iids = instruments().getInstrumentIds(getStringVar("instruments"));
		
		container.subscribeToMarketBidAskMessages();
		container.subscribeToMarketLastMessages();
		//container.subscribeToTheoMessages();
		//container.subscribeToOrderMessages();
		
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
		if (msg.instrumentId.equals("ZS-20140814-F")) {
			log(String.format("%1$s  MarketBidAsk: %2$d x %3$.3f    %4$.3f x %5$d", msg.instrumentId, prices.bid_size, prices.bid, prices.ask, prices.ask_size));
			container.getProbe("_bid").set(prices.bid);
			container.getProbe("_ask").set(prices.ask);
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
		//log("I have received a last price event for " + msg.instrumentId);
		if (msg.instrumentId.equals("ZS-20140814-F")) {
			container.getProbe("_last_price").set(msg.price);
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
		//log("I have recieved a THEO event for " + msg.instrumentId);
		Greeks greeks = theoService.getGreeks(msg.instrumentId);
		log(String.format("Delta:%1$.2f  Gamma:%2$.2f  Theta:%3$.2f  Vega:%4$.2f  Rho:%5$.2f", greeks.delta, greeks.gamma, greeks.theta, greeks.vega, greeks.rho));
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

} // class
