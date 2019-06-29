using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using pd.trading.lib;

namespace pd.options.lib
{
    /// <summary>
    /// An OCMessage is a message received FROM OptionsCity.
    /// </summary>
    public class OCMessage
    {
        protected string _header;
        protected string[] _fields;

        public string Header
        {
            get { return _header; }
        }

        public string[] Fields
        {
            get { return _fields; }
        }

        public OCMessage(string message)
        {
            // Split up the message fields using spaces as delimeters.
            string[] fields = message.Split(new char[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);

            // Header is the first message field.
            _header = fields[0];

            // All fields after the header will be exposed as the remaining fields in our message.
            _fields = new string[fields.Length - 1];
            for (int i = 1; i < fields.Length; i++)
            {
                _fields[i - 1] = fields[i];
            }
        }
    } // class

    /*public class SupportedInstrumentsMessage: OCMessage
    {

    } // class*/

    public class OCInstrument
    {
        protected InstrumentType _instrumentType;
        private PDInstrument _instrument = null;

        public InstrumentType InstrumentType { get { return _instrumentType; } }
        public PDInstrument Instrument { get { return _instrument; } }

        public OCInstrument(string[] fields, ref int i)
        {
            string instrumentType = fields[i];
            ++i;

            _instrumentType = InstrumentType.UNKNOWN;
            if (instrumentType.Equals("C"))
            {
                _instrumentType = InstrumentType.CALL;
                _instrument = new Option(fields, ref i);
            }
            else if (instrumentType.Equals("P"))
            {
                _instrumentType = InstrumentType.PUT;
                _instrument = new Option(fields, ref i);
            }
            else if (instrumentType.Equals("F"))
            {
                _instrumentType = InstrumentType.FUTURE;
                _instrument = new Future(fields, ref i);
            }
            else if (instrumentType.Equals("E"))
            {
                _instrumentType = InstrumentType.EQUITY;
                _instrument = new Equity(fields, ref i);
            }
            else if (instrumentType.Equals("I"))
            {
                _instrumentType = InstrumentType.INDEX;
                _instrument = new Index(fields, ref i);
            }
            else if (instrumentType.Equals("S"))
            {
                _instrumentType = InstrumentType.SPREAD;
                _instrument = new Spread(fields, ref i);
            }

        }
    } // class


} // namespace
