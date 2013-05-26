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
import tkMessageBox
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2TkAgg

from random import randint

class CarGui:

    def __init__(self, master, endCmd):
        # Intercept window deletion to ensure proper cleanup
        master.protocol("WM_DELETE_WINDOW", endCmd)

        # Create LabelFrame for Car Data
        lfr_data = LabelFrame(master, text="Data From Car", padx=5, pady=5)
        lfr_data.pack(fill="both", expand="yes")

        self.lbl_left = Label(lfr_data, text="Inside the labelframe")
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
        fr_buttons.pack()

    def arm(self):
        print "CAUTION, THE CAR IS ARMED"
        self.lbl_left["text"] = self.lbl_left["text"] + "\n\nArmed!"

        x = np.arange(0.0,3.0,0.01)
        y = np.sin(2*np.pi*x + randint(1, 20))
        self.plot(x, y)

    def plot(self, x, y):
        plt.clf()
        plt.plot(x, y)
        plt.gcf().canvas.draw()

    def handleMsg(self, msg):
        print msg

## END class CarGui


class CarApp:
    def __init__(self, master):
        self.master = master
        # Create a Queue between this app and the Comms link
        self.queue = Queue.Queue()

        # Create the GUI
        self.gui = CarGui(master, self.endApp)

        # Setup the threads (2: comms link, worker thread)
        self.running      = 1
        self.commsLink    = CommsLink(master, 50007, self.queue)
        self.commsThread  = threading.Thread(target=self.commsLink.connect)
        self.workerThread = threading.Thread(target=self.processIncoming)
        # Set them up as daemons
        self.commsThread.daemon = True;
        self.workerThread.daemon = True;

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
            #TODO: process message
            self.gui.handleMsg(msg)

## END class CarApp


class CommsLink:
    def __init__(self, master, port, queue):
        self.port  = port
        self.queue = queue
        self.connected = 0

    def disconnect(self):
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
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('', self.port))
        s.listen(1)
        conn, addr = s.accept()
        print 'Connected by', addr
        self.connected = 1

        # AFter connected, receive messages until disconnected
        while (self.connected):
            msg = self.receive(conn)
            if not msg: self.disconnect()
            self.queue.put(msg)

        print "Comms Link no longer connected, exiting!"
        conn.close()

    def receive(self, connection):
        # Receive a message
        return connection.recv(1024)

    def send(self, msg):
        # TODO: send a message
        print "Send Function:"

## END class CommsLink


root = Tk()
CarApp(root)

root.mainloop()

