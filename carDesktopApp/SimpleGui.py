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

class CarGui:

    def __init__(self, master, endCmd):
        # Create LabelFrame for Car Data
        data_labelframe = LabelFrame(master, text="Data From Car", padx=5, pady=5)
        data_labelframe.pack(fill="both", expand="yes")

        lf_left = Label(data_labelframe, text="Inside the labelframe")
        lf_left.pack(padx=10, pady=10)

        # Create Frame for Buttons
        frame = Frame(master)
        frame.pack()

        self.button = Button(frame, text="QUIT", command = endCmd) 
        self.button.pack(side=LEFT)

        self.hi_there = Button(frame, text="ARM", command=self.arm)
        self.hi_there.pack(side=LEFT)

    def arm(self):
        print "CAUTION, THE CAR IS ARMED"

    def handleMsg(self, msg):
        print msg

class CarApp:
    def __init__(self, master):
        self.master = master
        # Create a Queue between this app and the Comms link 
        self.queue = Queue.Queue()

        # Create the GUI
        self.gui = CarGui(master, self.endApp)

        # Setup the threads (2: comms link, worker thread)
        self.running      = 1
        self.commsLink    = CommsLink(master, 5001, self.queue)
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
    seperate than the main App thread!
    """
    def processIncoming(self):
        print "Worker thread starting up!"
        while self.running:
            msg = self.queue.get()
            #TODO: process message
            self.gui.handleMsg(msg)


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
        # TODO: Connect to the socket
        self.connected = 1

        # AFter connected, receive messages until disconnected
        while (self.connected):
            msg = self.receive()
            self.queue.put(msg)

        print "Comms Link no longer connected, exiting!"

    def receive(self):
        # TODO: receive a message

        # simulating message reception with sleep call
        time.sleep(1)

        return "new msg received..."

    def send(self, msg):
        # TODO: send a message
        print "Send Function:"

root = Tk()
app  = CarApp(root)

root.mainloop()
