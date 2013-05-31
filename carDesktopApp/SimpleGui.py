#!/usr/bin/python
# ==================================
# The beginnings of a simple GUI to
# arm the rocket on the car and
# display data.
# ==================================
from Tkinter import *
import threading
import Queue
import time
import sys
import socket
import re
import tkMessageBox
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2TkAgg

from random import randint

class CarGui:

    def __init__(self, master, endCmd, commsLink):
        self.commsLink  = commsLink
        self.accel_list = []
        self.lat_list   = []
        self.lon_list   = []
       
        # Intercept window deletion to ensure proper cleanup
        master.protocol("WM_DELETE_WINDOW", endCmd)

        # Create LabelFrame for Car Data
        lfr_data = LabelFrame(master, text="Data From Car", padx=5, pady=5)
        lfr_data.pack(fill="both", expand="yes")

        self.lbl_left = Label(lfr_data, text="Car unarmed")
        self.lbl_left.pack(padx=10, pady=10)

        # Create Frame for figure
        fr_figure = Frame(lfr_data)
        fr_figure.pack(fill="both", expand="yes")
        fig = plt.figure()
        
        canvas  = FigureCanvasTkAgg(fig, master=fr_figure)
        toolbar = NavigationToolbar2TkAgg(canvas, fr_figure)
        canvas.get_tk_widget().grid(row=0, column=1)
        toolbar.grid(row=1, column=1)

        # Create Frame for buttons
        fr_buttons = Frame(master)
        self.btn_quit = Button(fr_buttons, text="QUIT", command=endCmd)
        self.btn_quit.pack(side=LEFT)

        self.btn_arm = Button(fr_buttons, text="ARM", command=self.arm)
        self.btn_arm.pack(side=LEFT)

        self.btn_reset = Button(fr_buttons, text="RESET", command=self.reset)
        self.btn_reset.pack(side=LEFT)
        fr_buttons.pack()

    def arm(self):
        self.commsLink.send('ARM')
        print "CAUTION, THE CAR IS ARMED"
        self.lbl_left["text"] = "Armed!"

    def reset(self):
        self.commsLink.send('RESET')
        print "RESET MESSAGE SENT TO PHONE"
        self.lbl_left["text"] = "RESET sent. Car unarmed."

    def plot(self, x, y):
        plt.clf()
        plt.plot(x, y)
        plt.gcf().canvas.draw()

    def plot_latlong(self, x1, y1, x2, y2):
        plt.clf()
        plt.plot(x1, y1, x2, y2)
        plt.gcf().canvas.draw()

    def handleMsg(self, msg):
        if (len(msg) == 1):
            self.accel_list += [msg[0]]
            if (len(self.accel_list) % 20) == 0:
                self.plot(list(xrange(len(self.accel_list))), self.accel_list)
        elif (len(msg) == 2):
            self.lat_list += [msg[0]]
            self.lon_list += [msg[1]]
            latlong_axes = list(xrange(len(self.lat_list)))
            self.plot_latlong(latlong_axes, self.lat_list, latlong_axes, self.lon_list)



## END class CarGui


class CarApp:
    def __init__(self, master):
        self.master = master
        # Create a Queue between this app and the Comms link
        self.queue = Queue.Queue()

        # Setup the threads (2: comms link, worker thread)
        self.running      = 1
        self.commsLink    = CommsLink(master, self.queue)
        self.commsThread  = threading.Thread(target=self.commsLink.connect)
        self.workerThread = threading.Thread(target=self.processIncoming)
        
        # Set them up as daemons
        self.commsThread.daemon = True;
        self.workerThread.daemon = True;

        # Create the GUI
        self.gui = CarGui(master, self.endApp, self.commsLink)
        
        # Start the worker first so he is prepped to receive commands
        # then open the comms link
        self.workerThread.start()
        self.commsThread.start()

    def endApp(self):
        self.commsLink.disconnect()
        self.running = 0
        sys.exit(1)

    """
    Handle Incoming messages on our queue COMMSLINK=>CARAPP(Worker),
    This invokes GUI processing corresponding to the received message
    in a new thread so we don't slow down the UI.  This thread is
    separate than the main App thread!
    """
    def processIncoming(self):
        print "Worker thread starting up!"
        while self.running:
            msg = self.queue.get()
    
            # Check for different possible message types,
            # have a match object for each one.
            # Match object for Latitude, Longitude messages
            matchLatLong = re.match( r'LAT:([\-+]\d+\.\d+)\sLON:([\-+]\d+\.\d+)', msg)

            # Match object for acceleration messages
            matchAccel = re.match(r'ACC:([\-+]\d+\.\d+)', msg)

            if matchAccel:
                self.gui.handleMsg([float(matchAccel.group(1))])
            elif matchLatLong:    
                self.gui.handleMsg([float(matchLatLong.group(1)), float(matchLatLong.group(2))])

## END class CarApp


class CommsLink:
    def __init__(self, master, queue):
        self.ADDR      = ''
        self.PORT      = 50007 
        self.queue     = queue
        self.connected = 0
        self.socket    = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.conn      = None

    def disconnect(self):
        if self.conn:
            self.conn.close()
        self.connected = 0

    """
    This function connects to the android app and receives data.
    This function is invoked by the main app in its own thread. Any
    received data is put on the message queue, and then it attempts to
    receive the next message.  Any processing of the message is done
    by another thread.
    """
    def connect(self):
        # Connect to the socket
        self.socket.bind((self.ADDR, self.PORT))
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.listen(1)
        self.conn, addr = self.socket.accept()
        print 'Connected by', addr
        self.connected = 1

        # After connected, receive messages until disconnected
        while (self.connected):
            msg = self.receive()
            if not msg:
                print "Comms Link no longer connected, exiting!"
                self.connected = 0
                #self.disconnect()
            if not ':' in msg:
                print msg
            self.queue.put(msg)

    def receive(self):
        # Receive a message
        return self.conn.recv(20)

    def send(self, msg):
        self.conn.sendall(msg)

## END class CommsLink


root = Tk()
CarApp(root)

root.mainloop()

