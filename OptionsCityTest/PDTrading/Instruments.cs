using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace pd.trading.lib
{
    public enum InstrumentType { UNKNOWN, CALL, PUT, FUTURE, EQUITY, INDEX, SPREAD };

    public class PDInstrument
    {
        protected int _instrumentID;
        public int InstrumentID { get { return _instrumentID; } }
    } // class

    /// <summary>
    /// Option instrument
    /// </summary>
    public class Option : PDInstrument
    {
        #region Private Fields
        private string _parentSymbol;
        private string _classSymbol;
        private int _classID;
        private string _groupSymbol;
        private int _groupID;
        private double _strike;
        private string _expiration;
        private int _monthID;
        private int _underlyingInstrumentID;
        private double _minPriceIncrement;
        private string _exchange;
        #endregion

        #region Public Fields
        public string ParentSymbol { get { return _parentSymbol; } }
        public string ClassSymbol { get { return _classSymbol; } }
        public int ClassID { get { return _classID; } }
        public string GroupSymbol { get { return _groupSymbol; } }
        public int GroupID { get { return _groupID; } }
        public double Strike { get { return _strike; } }
        public string Expiration { get { return _expiration; } }
        public int MonthID { get { return _monthID; } }
        public int UnderlyingInstrumentID { get { return _underlyingInstrumentID; } }
        public double MinPriceIncrement { get { return _minPriceIncrement; } }
        public string Exchange { get { return _exchange; } }
        #endregion

        /// <summary>
        /// If you pass in the fields to this constructor and give a starting index for the fields to use,
        /// this will construct an Option instrument.
        /// </summary>
        /// <param name="fields">string array of message fields</param>
        /// <param name="i">starting index in fields array to begin option instrument data</param>
        public Option(string[] fields, ref int i) : this(fields[i], fields[++i], int.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), int.Parse(fields[++i]), int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i]) { ++i; }

        public Option(string parentSymbol, string classSymbol, int classID, string groupSymbol, int groupID, double strike, string expiration, int monthID, int instrumentID, int underlyingInstrumentID, double minPriceIncrement, string exchange)
        {
            _parentSymbol = parentSymbol;
            _classSymbol = classSymbol;
            _classID = classID;
            _groupSymbol = groupSymbol;
            _groupID = groupID;
            _strike = strike;
            _expiration = expiration;
            _monthID = monthID;
            _instrumentID = instrumentID;
            _underlyingInstrumentID = underlyingInstrumentID;
            _minPriceIncrement = minPriceIncrement;
            _exchange = exchange;
        }

        public override string ToString()
        {
            return string.Format("[iID:{0}] [uiID:{1}] [cID:{2}] [gID:{3}] {4} {5} [mID:{6}] {7} {8} {9} {10} {11}", _instrumentID, _underlyingInstrumentID, _classID, _groupID, _strike, _expiration, _monthID, _parentSymbol, _classSymbol, _groupSymbol, _minPriceIncrement, _exchange);
        }
    } // class

    /// <summary>
    ///  Future instrument
    /// </summary>
    public class Future : PDInstrument
    {
        #region Private Fields
        private string _parentSymbol;
        private string _classSymbol;
        private int _classID;
        private string _groupSymbol;
        private int _groupID;
        private string _expiration;
        private int _monthID;
        private double _minPriceIncrement;
        private string _exchange;
        #endregion

        #region Public Fields
        public string ParentSymbol { get { return _parentSymbol; } }
        public string ClassSymbol { get { return _classSymbol; } }
        public int ClassID { get { return _classID; } }
        public string GroupSymbol { get { return _groupSymbol; } }
        public int GroupID { get { return _groupID; } }
        public string Expiration { get { return _expiration; } }
        public int MonthID { get { return _monthID; } }
        public double MinPriceIncrement { get { return _minPriceIncrement; } }
        public string Exchange { get { return _exchange; } }
        #endregion

        /// <summary>
        /// If you pass in the fields to this constructor and give a starting index for the fields to use,
        /// this will construct a Future instrument.
        /// </summary>
        /// <param name="fields">string array of message fields</param>
        /// <param name="i">starting index in fields array to begin future instrument data</param>
        public Future(string[] fields, ref int i) : this(fields[i], fields[++i], int.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i]) { ++i; }

        public Future(string parentSymbol, string classSymbol, int classID, string groupSymbol, int groupID, string expiration, int monthID, int instrumentID, double minPriceIncrement, string exchange)
        {
            _parentSymbol = parentSymbol;
            _classSymbol = classSymbol;
            _classID = classID;
            _groupSymbol = groupSymbol;
            _groupID = groupID;
            _expiration = expiration;
            _monthID = monthID;
            _instrumentID = instrumentID;
            _minPriceIncrement = minPriceIncrement;
            _exchange = exchange;
        }

        public override string ToString()
        {
            return string.Format("[iID:{0}] [cID:{1}] [gID:{2}] {3} [mID:{4}] {5} {6} {7} {8} {9}", _instrumentID,  _classID, _groupID, _expiration, _monthID, _parentSymbol, _classSymbol, _groupSymbol, _minPriceIncrement, _exchange);
        }

    } // class

    /// <summary>
    /// Equity instrument
    /// </summary>
    public class Equity : PDInstrument
    {
        #region Private Fields
        private string _symbol;
        private double _minPriceIncrement;
        private string _exchange;
        #endregion

        #region Public Fields
        public string Symbol { get { return _symbol; } }
        public double MinPriceIncrement { get { return _minPriceIncrement; } }
        public string Exchange { get { return _exchange; } }
        #endregion

        /// <summary>
        /// If you pass in the fields to this constructor and give a starting index for the fields to use,
        /// this will construct an Equity instrument.
        /// </summary>
        /// <param name="fields">string array of message fields</param>
        /// <param name="i">starting index in fields array to begin future instrument data</param>
        public Equity(string[] fields, ref int i) : this(fields[i], int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i]) { ++i;  }

        public Equity(string symbol, int instrumentID, double minPriceIncrement, string exchange)
        {
            _symbol = symbol;
            _instrumentID = instrumentID;
            _minPriceIncrement = minPriceIncrement;
            _exchange = exchange;
        }

        public override string ToString()
        {
            return string.Format("[iID:{0}] {1} {2} {3}", _instrumentID, _symbol, _minPriceIncrement, _exchange);
        }

    } // class

    /// <summary>
    /// Index instrument
    /// </summary>
    public class Index : PDInstrument
    {
        #region Private Fields
        private string _symbol;
        private double _minPriceIncrement;
        private string _exchange;
        #endregion

        #region Public Fields
        public string Symbol { get { return _symbol; } }
        public double MinPriceIncrement { get { return _minPriceIncrement; } }
        public string Exchange { get { return _exchange; } }
        #endregion

        public Index(string[] fields, ref int i) : this(fields[i], int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i]) { ++i;  }

        public Index(string symbol, int instrumentID, double minPriceIncrement, string exchange)
        {
            _symbol = symbol;
            _instrumentID = instrumentID;
            _minPriceIncrement = minPriceIncrement;
            _exchange = exchange;
        }

        public override string ToString()
        {
            return string.Format("[iID:{0}] {1} {2} {3}", _instrumentID, _symbol, _minPriceIncrement, _exchange);
        }

    } // class

    public class SpreadLeg
    {
        private int _instrumentID;
        private double _legRatio;

        public int InstrumentID { get { return _instrumentID; }}
        public double LegRatio { get { return _legRatio; }}

        public SpreadLeg(int instrumentID, double legRatio)
        {
            _instrumentID = instrumentID;
            _legRatio = legRatio;
        }
    }

    /// <summary>
    ///  Spread instrument
    /// </summary>
    public class Spread : PDInstrument
    {
        #region Private Fields
        private string _parentSymbol;
        private string _classSymbol;
        private int _classID;
        private string _groupSymbol;
        private int _groupID;
        private string _expiration;
        private int _monthID;
        private double _minPriceIncrement;
        private string _exchange;
        private int _legCount;
        private List<SpreadLeg> _legs;
        #endregion

        #region Public Fields
        public string ParentSymbol { get { return _parentSymbol; } }
        public string ClassSymbol { get { return _classSymbol; } }
        public int ClassID { get { return _classID; } }
        public string GroupSymbol { get { return _groupSymbol; } }
        public int GroupID { get { return _groupID; } }
        public string Expiration { get { return _expiration; } }
        public int MonthID { get { return _monthID; } }
        public double MinPriceIncrement { get { return _minPriceIncrement; } }
        public string Exchange { get { return _exchange; } }
        public int LegCount { get { return _legCount; } }
        public List<SpreadLeg> Legs { get { return _legs; } }
        #endregion

        /// <summary>
        /// If you pass in the fields to this constructor and give a starting index for the fields to use,
        /// this will construct a Future instrument.
        /// </summary>
        /// <param name="fields">string array of message fields</param>
        /// <param name="i">starting index in fields array to begin future instrument data</param>
        public Spread(string[] fields, ref int i) : this(fields[i], fields[++i], int.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]), int.Parse(fields[++i]), double.Parse(fields[++i]), fields[++i], int.Parse(fields[++i]))
        { 
            _legs = new List<SpreadLeg>();

            for (int j=0; j<_legCount; j++)
            {
                int instrumentID = int.Parse(fields[++i]);
                double legRatio = double.Parse(fields[++i]);
                
                SpreadLeg leg = new SpreadLeg(instrumentID, legRatio);
                _legs.Add(leg);
            }

            ++i;
        }

        public Spread(string parentSymbol, string classSymbol, int classID, string groupSymbol, int groupID, string expiration, int monthID, int instrumentID, double minPriceIncrement, string exchange, int legCount)
        {
            _parentSymbol = parentSymbol;
            _classSymbol = classSymbol;
            _classID = classID;
            _groupSymbol = groupSymbol;
            _groupID = groupID;
            _expiration = expiration;
            _monthID = monthID;
            _instrumentID = instrumentID;
            _minPriceIncrement = minPriceIncrement;
            _exchange = exchange;
            _legCount = legCount;
        }


    } // class


} // namespace
