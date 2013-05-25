# ==================================
# The beginnings of a simple GUI to 
# arm the rocket on the car and 
# display data. 
# ==================================
from Tkinter import *

class App:

	def __init__(self, master):
		frame = Frame(master)
		frame.pack()

		self.button = Button(frame, text="QUIT", command = frame.quit)
		self.button.pack(side=LEFT)

		self.hi_there = Button(frame, text="ARM", command=self.arm)
		self.hi_there.pack(side=LEFT)

	def arm(self):
		print "CAUTION, THE CAR IS ARMED"

root = Tk()
app  = App(root)

root.mainloop()
