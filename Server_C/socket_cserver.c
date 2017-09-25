#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <sys/socket.h>
#include <unistd.h>
#include <arpa/inet.h>

#define BUFSIZE 2048
#define SERVICE_PORT	48010
#define PKTSIZE 1000
#define MAXFRAME 10
#define MAXPKT 5


int
main(int argc, char **argv)
{
	struct sockaddr_in myaddr;	/* our address */
	struct sockaddr_in remaddr;	/* remote address */
	socklen_t addrlen = sizeof(remaddr);		/* length of addresses */
	int recvlen;			/* # bytes received */
	int fd;				/* our socket */
	int msgcnt = 0;			/* count # of messages we received */
	unsigned char buf[BUFSIZE];	/* receive buffer */
	unsigned char* whole_frame = (char*)malloc(12100);


	/* create a UDP socket */
	if ((fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
		perror("cannot create socket\n");
		return 0;
	}

	/* bind the socket to any valid IP address and a specific port */
	memset((char *)&myaddr, 0, sizeof(myaddr));
	myaddr.sin_family = AF_INET;
	myaddr.sin_addr.s_addr = htonl(INADDR_ANY);
	myaddr.sin_port = htons(SERVICE_PORT);

	if (bind(fd, (struct sockaddr *)&myaddr, sizeof(myaddr)) < 0) {
		perror("bind failed");
		return 0;
	}
  
  int frame_id = 0;
  int packet_id = 1;

  char pkt[PKTSIZE];
  memset(pkt, sizeof(pkt), 1);
  /* Override the loop, sending the video frames */
  recvlen = recvfrom(fd, buf, BUFSIZE, 0, (struct sockaddr *)&remaddr, &addrlen);
  while (1) {
    itoa(frame_id, pkt);

    sleep(1);
    for (int i = 0; i < MAXPKT; i = i + 1) {
      itoa(i, pkt + 4);
      n = sendto(fd, pkt, strlen(pkt), 0, &remaddr, addrlen);
      if (n < 0)
        error("ERROR in sendto");
    }
    frame_id = frame_id + 1;
    if (frame_id == MAXFRAME)
      break;
  }


  return 0;


	for (;;)
	{
		int last_id = 0;
		/* now loop, receiving data and printing what we received */
		int total_size_received = 0;
		while (last_id == 0) 
		{
			printf("waiting on port %d\n", SERVICE_PORT);
			recvlen = recvfrom(fd, buf, BUFSIZE, 0, (struct sockaddr *)&remaddr, &addrlen);
			if (recvlen > 0) {
				buf[recvlen] = '\0';

				memcpy(whole_frame+total_size_received, buf+4, recvlen-4);
				total_size_received += recvlen-4;

				short* frame_id = (short*)malloc(sizeof(short));
				*frame_id = 0;
				memcpy(frame_id, buf, 2);
				short* segment_id = (short*)malloc(sizeof(short));
				*segment_id = 0;
				memcpy(segment_id, buf + 2, 1);
				short* last_segment_tag = (short*)malloc(sizeof(short));
				*last_segment_tag = 0;
				memcpy(last_segment_tag, buf + 3, 1);
				last_id = *last_segment_tag;
				printf("received message frame id: %hi, segment id: %hi, last segment flag: %hi\n", *frame_id, *segment_id, *last_segment_tag);
			}
			else
				printf("uh oh - something went wrong!\n");
		}

		printf("!!!!!Received one whole frame!!!!! length is: %d\n", total_size_received);
	}
	/* never exits */
}
