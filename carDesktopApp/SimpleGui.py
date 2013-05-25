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

		canvas = Canvas(master, width=200, height=100)
		canvas.pack(side=TOP)
		canvas.create_rectangle(50, 25, 150, 75)

	def arm(self):
		print "CAUTION, THE CAR IS ARMED"

root = Tk()
app  = App(root)

root.mainloop()
