using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace pd.trading.lib
{
    public delegate void TradeDelegate();
    public delegate void InstrumentFoundDelegate(PDInstrument instrument);
    public delegate void ConnectedDelegate();
    public delegate void DisconnectedDelegate();

    public interface ITrading
    {
        void Connect();
        void Disconnect();
        event TradeDelegate TradeEvent;
        event InstrumentFoundDelegate InstrumentFoundEvent;
        event ConnectedDelegate ConnectedEvent;
        event DisconnectedDelegate DisconnectedEvent;
    } // interface

} // namespace
