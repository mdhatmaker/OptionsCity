using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.Windows.Forms;
using System.Threading;
using pd.trading.lib;

namespace pd.options.lib
{
    public class OCTradingPlatform : ITrading
    {
        public event TradeDelegate TradeEvent;
        public event InstrumentFoundDelegate InstrumentFoundEvent;
        public event ConnectedDelegate ConnectedEvent;
        public event DisconnectedDelegate DisconnectedEvent;

        const string address = "10.46.48.54";
        const int port = 9031;
        //const string user = "MN8";
        const string user = "NVK";
        //const string password = "nashed";
        const string password = "novak";
        const string version = "V2.3";
        const int accountType = 0;

        private Socket _sock;
        private List<string> _ocMessages;
        private string _user;
        private string _password;
        private string _version;
        private int _accountType;        // 0 = normal user, 1 = normal API, 2 = master API
        private List<OCInstrument> _instruments = new List<OCInstrument>();

        public static ToolStripStatusLabel Status { get; set; }
        public static ListBox Output { get; set; }

        public List<OCInstrument> Instruments { get { return _instruments; } }

        private static OCTradingPlatform _instance;
        private static Thread _listenThread;
        private static bool _isConnected = false;

        public static ITrading Instance
        {
            get
            {
                if (_instance == null)
                {
                    _instance = new OCTradingPlatform();
                    _listenThread = new Thread(new ThreadStart(_instance.Receive));
                }
                return _instance;
            }
        }

        private OCTradingPlatform()
        {
            print("creating socket...");
            _sock = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        }

        public void Connect()
        {
            //sock = OCSocket.Instance;

            print("connecting...");
            // returns exception message if fails....null if no exception
            List<string> connectMsg = SocketConnect(address, port);
            if (connectMsg != null)
            {
                print(connectMsg);
                return;
            }
            print("socket connected.");

            //EnableHeartbeats(false);

            //send("GetLoggedOnUsers");
            //receive();
            //InstrumentFoundEvent += new InstrumentFoundDelegate(OnInstrumentFound);
            //sock.GetLoggedOnUsers();

            ForceLogoff(user);

            //sock.EnableHeartbeats(false);

            print("logging on...");
            Logon(user, password, version, accountType);
            /*List<string> initialInfo = sock.Logon(user, password, version, accountType);
            print(initialInfo);
            sock.ParseMessages(initialInfo);*/

            //print(string.Format("Instrument count: {0}", sock.Instruments.Count));

            TheoValuesRequest(1870157);
            TheoValuesRequestWithRisk(1870157);

            /*foreach (OCInstrument oci in sock.Instruments)
            {
                listInstruments.Items.Add(oci.Instrument.ToString());
            }*/
        }

        public void Disconnect()
        {
        }

        private void print(string msg)
        {
            System.Console.WriteLine(msg);
        }

        private void print(List<string> messages)
        {
            foreach (string msg in messages)
            {
                print(msg);
            }
        }

        private void Logon(string user, string password, string version, int accountType)
        {
            string logonMsg = "LogOn " + user + " " + password + " " + version + " " + accountType.ToString();

            _user = user;
            _password = password;
            _version = version;
            _accountType = accountType;

            Send(logonMsg);
            //return(Receive());
        }

        private void Logoff()
        {
            Send("LogOff");
        }

        private void ForceLogoff(string user)
        {
            Send("LogOffUser " + user);
        }

        private void HeartbeatAck()
        {
            Send("HeartbeatAck");
        }

        private void EnableHeartbeats(bool b)
        {
            if (b == true)
                Send("EnableHeartbeats");
            else
                Send("DisableHeartbeats");
        }

        private List<string> SocketConnect(string address, int port)
        {
            List<string> result = null;

            try
            {
                if (_isConnected == false)
                {
                    _sock.Connect(address, port);
                    _isConnected = true;

                    _listenThread.Start();
                }
            }
            catch (SocketException ex)
            {
                result = new List<string>();
                result.Add(ex.Message);
            }
            return result;
        }

        public void SocketDisconnect()
        {
            _listenThread.Abort();
            _instance.EnableHeartbeats(true);
            _instance.Logoff();
            _sock.Close();
            _isConnected = false;
        }

        private void Close()
        {
            _sock.Close();
        }

        private void GetLoggedOnUsers()
        {
            Send("GetLoggedOnUsers");
            //return Receive();
        }

        public void TheoValuesRequest(int instrumentID)
        {
            Send(string.Format("TheoValuesRequest {0}", instrumentID));
            //return Receive();
        }

        public void TheoValuesRequestWithRisk(int instrumentID)
        {
            Send(string.Format("TheoValuesRequestWithRisk {0}", instrumentID));
            //return Receive();
        }

        private void Send(string msg)
        {
            try
            {
                print(msg);
                byte[] bytes = System.Text.Encoding.ASCII.GetBytes(msg + "\n");
                lock (this)
                {
                    _sock.Send(bytes);
                }
            }
            catch (SocketException ex)
            {
                System.Console.WriteLine(ex.Message);
            }
        }

        private void Receive()
        {
            while (true)
            {
                _ocMessages = new List<string>();

                string msg;
                StringBuilder sb = new StringBuilder();

                int iCount = 0;
                do
                {
                    // Convert the buffer of bytes into a string.
                    byte[] buffer = new byte[4096];

                    lock (this)
                    {
                        try
                        {
                            iCount = _sock.Receive(buffer);
                        }
                        catch (SocketException ex)
                        {
                            print(ex.Message);
                        }
                    }

                    msg = System.Text.Encoding.ASCII.GetString(buffer);
                    msg = msg.Substring(0, iCount);

                    if (msg.Contains('\n'))
                    {
                        // Split the message at each newline character.
                        string[] messages = msg.Split(new char[] { '\n' }, StringSplitOptions.RemoveEmptyEntries);

                        foreach (string s in messages)
                        {
                            _ocMessages.Add(s.TrimEnd(new char[] { '\n' }));
                            Thread.Sleep(1);
                        }

                        ParseMessages(_ocMessages);

                    }
                    sb.Append(msg);

                    print(string.Format("{0} bytes", iCount));
                    Thread.Sleep(1);

                    

                } while (iCount > 0);

                if (sb.Length > 0)
                {
                    msg = sb.ToString();

                    // Split the message at each newline character.
                    string[] messages = msg.Split(new char[] { '\n' }, StringSplitOptions.RemoveEmptyEntries);

                    foreach (string s in messages)
                    {
                        _ocMessages.Add(s.TrimEnd(new char[] { '\n' }));
                        Thread.Sleep(1);
                    }

                    ParseMessages(_ocMessages);
                    Thread.Sleep(1);
                }
                else
                {
                    print("sleeping...");
                    Thread.Sleep(100);
                }
            }
        }

        private void ParseMessages(List<string> messages)
        {
            foreach (string msg in messages)
            {
                if (!msg.StartsWith("SupportedInstruments"))
                    print(msg);

                OCMessage ocm = new OCMessage(msg);
                // Heartbeat
                if (ocm.Header.Equals("Heartbeat"))
                {
                    HeartbeatAck();
                    //EnableHeartbeats(false);
                }
                // SupportedInstruments
                else if (ocm.Header.Equals("SupportedInstruments"))
                {
                    int i = 0;

                    do
                    {
                        OCInstrument instrument = new OCInstrument(ocm.Fields, ref i);
                        _instruments.Add(instrument);
                        if (InstrumentFoundEvent != null)
                            InstrumentFoundEvent(instrument.Instrument);
                    } while (i < ocm.Fields.Length);
                }
                else if (ocm.Header.Equals("LogOnAck"))
                {
                    EnableHeartbeats(false);
                }

                Thread.Sleep(1);
            }
        }

    } // class


} // namespace
