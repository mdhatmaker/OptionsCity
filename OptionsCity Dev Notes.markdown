#OptionsCity Freeway Development Notes

##Questions/Answers (from OptionsCity tech support)
Answers in Red below, along with your additional question.  You can email freewaysupport@optionscity.com and can expect very timely responses from them.  I'm happy to get involved, but am usually booked all day and then some.  Sorry for any delays in responses, but please do email the address above in the future.  They will come to me if there is something they are unable to answer.

**1) I see that for the theoService.getVolatilityCurve(instrumentMonth), the instrumentMonth format is supposed to be "parentsymbol_type_expiration". Could I see a couple of (hypothetical) examples of this instrumentMonth string so I can more clearly understand how I should format the string?**

"ES_O_JUL14".  Expiration is the display expiration value.  For Strategies, "ES_S_JUL14"

**2) Also, with the charts in the dashboard, if I am charting a probe that I update on a timer (say every one minute), will the chart obtain data points each time I update this probe (even if the value of the probe stays the same)? Or does the value of the probe have to actually change in order for the chart to obtain a data point? More concisely, if I am updating my probe on a timer, will the chart obtain a data point each time I call probe.set() in my timer event code?**

http://utilities.optionscity.com/files/freeway/5.1/freeway_javadoc/   (Look up IProbe)

There is a method, setAlwaysSendValues().  You can set this to true and it will override behaviour to send values even if they are the same.  For performance reasons, it currently does not resend if the probe is not changing.

**3) Another dashboard chart question: I have some probes which I was using to publish at-the-money vols, and I changed the magnitude so that rather than .554 I am now publishing the vol as 55.4. But the chart is attempting to chart *all* the vols (both before and after I made this change), so I get a huge jump which I don't want (screenshot is attached). I tried to find a context-menu on the chart to "clear" or something similar. Also, I thought maybe there was a method on the probe itself in the code like a "clear()" method, but again no dice. At the simplest level, I would just like to clear a chart and tell it to begin anew to collect and display the probe data.**

The UI caches a configured amount of variables.  This can be 5 minutes, 15, etc.  You can clear probe values, but the UI will still have painted your old vols in this case.  Once you've passed the 5 minute window if configured for that, your UI should update and give a better default zoom / resolution.  You can also right-click and set auto-range so that when the old, smaller values have disappeared, it should zoom into the higher vol range.  This problem should've gone away, however, quickly if you had your window set to a lower time frame.  Probe values don't stay painted forever.

**4) Alas, one more chart question: How would I go about charting an X and Y value? I can publish my probes and view them on a chart relative to time. But let's say I want to chart a volatility smile that would have strikes on the X axis and the vols on the Y axis. How would I achieve this?**

Scatter Plot.  This UI widget is made exactly for this.  You should look into using grids; it's easier to see your values and you can also bind the various UI widgets to grids.  If you had a grid with an X column and a Y column, you could bind the scatter plot to it and you will see the smile.

**One other random question: What's the difference between adding a probe in the install vs the begin methods? It seems like I have used both setup.addProbe("xxx") in install and container.addProbe("xxx") in begin. Is there a fundamental difference between these two? Or is there a preferred way?**

Nothing.  Probes used to be able to be added in install() only.  Now they can be added dynamically.

**The "format" column in a dashboard Grid: How do I use this column? What are some examples of valid formats I can type in this column? In particular, I would like certain values to be integer (rather than defaulting to Double)**

**Is there any way for a Job to communicate with the "outside world" or is it completely "sandboxed"? For example, could I use Java networking libraries to establish a remote TCP/IP connection and send/receive information this way**


In the formate, you can do

number:$000.00;  take a look at http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html for more details

(-1000)-(-101)=red,(-100)-0=yellow,1-300=green

Also you can do more advanced formating

bar:0,20    Shows a progress bar that visual indicates a value from 0-20.

choice:fast=fast;med=med;slow=slow  Creates a dropdown with choices of "fast","med" and "slow"

choices:fast=fast;med=med;slow=slow     Creates a dropdown that allows multiple selection. Selection appears as a comma-separated list

checkbox        Shows a checkbox. The checkbox toggles between string values of "true" and "false". Setters and getters on these values should expect "true" and "false" strings.

---
Michael, 

http://209.249.193.106:8088/optionscity/optionscity/ is the url for your production freeway access. and Login will be MN10, the password will be the same as your dev instance for the same login username. Make sure that you have latest freeway on your server. http://utilities.optionscity.com/files/freeway/

Let me know if you have any question

Max 

--------------- Original Message ---------------
From: Eric Imperato [support@optionscity.com]
Sent: 8/20/2014 5:10 PM
To: freewaysupport@optionscity.com;
mark@mnafinancial.com
Subject: RE: production server for Freeway

Hi Michael, the login was created, identical to the one that Mark uses for the development server. For now I set his trade abilities to 'limited' just as a precaution while he moves his algos over to the new instance. Let us know when you want him switched over to a full trader. 
Eric

---
PORT 9041 IS OPEN FOR YOUR FREEWAY SOCKET CONNECTION
---
##Documentation Notes

###Probes 
Simply speaking, a probe carries data from the inside of a job to the rest of the Freeway interface. Probes can be set to display any variable that your job deals with as it runs. For example, if your job tracks the spread between the front month and second month ES futures, you would set a variable to (ES_front - ES_second), and then set the probe to the value of that variable. Then, every time the value changed, the script could update the value of the probe. Once the probe is configured with a live value, it can be viewed in the Freeway interface in 3 ways: graph, heatmap, and the 'probes' tab in on the OnRamp screen. 

###Grids
Grids are similar to an Excel sheet that has many rows and columns. Jobs can create a grid, and then update any item in a row or column. Grids are typically used for a group of related data – for example, a list of all positions and the greeks associated with it. Data from grids can be displayed with the heatmap, graph, scatter plot, or 3D map widget.

###Signals
Signals allow a user to pass a value into the script or to trigger a specific action. In Dashboard, the user can create a button that broadcasts a given signal (and optional value) to all jobs currently running. If a job is set to listen for the specific signal that the button broadcasts, it will then  run the OnSignal routine in the code. 

Buttons are useful for reacting to specific events in the market. For example, a user could configure the button to execute three legs of a specific spread. When market conditions or news events warrant a quick reaction, one button click can rapidly enter the trades simultaneously. A button could also easily be configured to change edge requirements or order size in a trading strategy without interruption in the job.


Each display grid is backed by an IGrid in the code which contains all of the data that is displayed in the widget.


---

##LAUNCHING PROBLEMS - TROUBLE LAUNCHING THE FREEWAY APP FROM THE WEBSITE BUTTON

http://209.249.193.106:8088/optionscity/optionscity/onramp/launch.jsp?app=onramp&profile=Freeway


