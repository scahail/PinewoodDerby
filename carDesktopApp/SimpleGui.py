#!/usr/bin/python
# ==================================
# The beginnings of a simple GUI to
# arm the rocket on the car and
# display data.
# ==================================
from mtTkinter import *

class App:

    def __init__(self, master):

        # Create LabelFrame for Car Data
        data_labelframe = LabelFrame(master, text="Data From Car", padx=5, pady=5)
        data_labelframe.pack(fill="both", expand="yes")

        lf_left = Label(data_labelframe, text="Inside the labelframe")
        lf_left.pack(padx=10, pady=10)

        # Create Frame for buttons
        frame = Frame(master)
        self.button = Button(frame, text="QUIT", command = frame.quit)
        self.button.pack(side=LEFT)

        self.hi_there = Button(frame, text="ARM", command=self.arm)
        self.hi_there.pack(side=LEFT)
        frame.pack()

    def arm(self):
        print "CAUTION, THE CAR IS ARMED"

root = Tk()
app  = App(root)

root.mainloop()
