# Echo client program
import socket

HOST = 'localhost'    # The remote host
PORT = 50007              # The same port as used by the server
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))
s.sendall('This is a test message.')
print 'Sent test message'
while 1:
   data = s.recv(1024)
   if not s.recv: break
   print data
s.close()
print 'Done.'
