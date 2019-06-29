using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Net;
using System.Net.Sockets;
using pd.options.lib;
using pd.trading.lib;

namespace pd.options.test
{
    public partial class Form1 : Form
    {
        ITrading _tradingPlatform;

        int _instrumentCount = 0;

        public Form1()
        {
            InitializeComponent();


            //OCSocket.Status = this.tsStatus;
            //OCSocket.Output = this.listOutput;

            string[] messages = new string[] 
            {
                "DisableHeartbeats",
                "EnableHeartbeats",
                "GetLoggedOnUsers",
                "HeartbeatAck",
                "LogOff",
                "LogOn <user> <password> <version> [accountType]",
                "RiskRequest <InstrumentID> <CalculationType> <UnderlyingPrice> <VolatilityOffset> <DayOffset>",
                "ModelSettingsSnapshotRequest <InstrumentClassID> <InstrumentMonthID>",
                "TheoValuesRequest <InstrumentID> [Underlying Price]",
                "TheoValuesRequestWithRisk <InstrumentID> [UnderlyingPrice]",
                "GetQuoteSettings <InstrumentClassID> [InstrumentID]",
                "TradeSnapshotRequest",
                "MarketSnapshot <InstrumentClassID> [InstrumentID]",
                "SubscribeClassMD <InstrumentClassID>",
                "UnsubscribeClassMD <InstrumentClassID>",
                "SubscribeMD <InstrumentClassID> <InstrumentID>",
                "UnsubscribeMD <InstrumentClassID> <InstrumentID>",
                "ActivateSymbol <i.e. IBM>",
                "LogOffUser <i.e. trader0>",
                "TradeSnapshotRequest"
            };

            cboMessage.Items.Clear();
            foreach (string s in messages)
            {
                cboMessage.Items.Add(s);
            }

            _tradingPlatform = OCTradingPlatform.Instance;
            _tradingPlatform.ConnectedEvent += new ConnectedDelegate(OnConnected);
            _tradingPlatform.DisconnectedEvent += new DisconnectedDelegate(OnDisconnected);
            _tradingPlatform.InstrumentFoundEvent += new InstrumentFoundDelegate(OnInstrumentFound);
        }

        private void btnConnect_Click(object sender, EventArgs e)
        {
            _tradingPlatform.Connect();

            btnConnect.Enabled = false;
            btnDisconnect.Enabled = true;
            btnSend.Enabled = true;
        }

        public void OnInstrumentFound(PDInstrument instrument)
        {
            AddInstrumentItem(instrument.ToString());
            tsStatus.Text = string.Format("Instruments: {0}", ++_instrumentCount);
        }

        public void OnConnected()
        {
            print("CONNECTED");
        }

        public void OnDisconnected()
        {
            print("DISCONNECTED");
        }

        private delegate void AddInstrumentItemDelegate(object item);
        private void AddInstrumentItem(object item)
        {
            if (this.listInstruments.InvokeRequired)
                this.listInstruments.Invoke(new AddInstrumentItemDelegate(this.AddInstrumentItem), item);
            else
                this.listInstruments.Items.Add(item);
        }

        #region Small utility print methods (display in listbox)
        private void print(List<string> messages)
        {
            foreach (string s in messages)
                print(s);
        }

        private void print(int i)
        {
            print(i.ToString());
        }

        private void print(string msg)
        {
            listOutput.Items.Insert(0, msg);
            Application.DoEvents();
        }
        #endregion

        private void btnDisconnect_Click(object sender, EventArgs e)
        {
            print("logging off and closing trading platform...");
            _tradingPlatform.Disconnect();
            print("done.");

            btnConnect.Enabled = true;
            btnDisconnect.Enabled = false;
            btnSend.Enabled = false;
        }

        private void btnSend_Click(object sender, EventArgs e)
        {
            print("SEND: " + cboMessage.Text);
            //sock.Send(cboMessage.Text);
        }

        private void btnClear_Click(object sender, EventArgs e)
        {
            listOutput.Items.Clear();
        }

        private void btnTheo_Click(object sender, EventArgs e)
        {
            int result;
            if (int.TryParse(listOutput.SelectedItem.ToString(), out result) == true)
            {
                //sock.TheoValuesRequest(result);
                //sock.TheoValuesRequestWithRisk(result);
            }
        }
    } // class
} // namespace
