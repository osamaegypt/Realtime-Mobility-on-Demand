# Import socket module
import socket
 
 
def Main():
    # local host IP '127.0.0.1'
    host = '206.189.16.110'
    #host = '127.0.0.1'
 
    # Define the port on which you want to connect
    port = 2004
 
    s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
 
    # connect to server on local computer
    s.connect((host,port))

    print("connected to remote server ")
 
    # message you send to server
    message = "ID_AST#60078#end"
    while True:

        print("connected to remote server ")
 
        # message sent to server
        s.sendall(message.encode('utf-8'))
 
        # messaga received from server
        data = s.recv(1024)
 
        # print the received message
        # here it would be a reverse of sent message
        print("Received from the server :"+str(data))
 
        # ask the client whether he wants to continue
        ans = input('\n send new data ...or enter n to break the program:')
        if ans == 'n':
            break
       
    # close the connection
    s.close()
 
if __name__ == '__main__':
    Main()
