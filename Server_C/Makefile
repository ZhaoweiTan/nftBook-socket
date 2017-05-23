CC=gcc
CFLAGS = -g

all: socket_cserver

socket_cserver: socket_cserver.o 
	$(CC) -o socket_cserver socket_cserver.o $(LIBS)

socket_cserver.o: socket_cserver.c

clean:
	rm -f socket_cserver socket_cserver.o
