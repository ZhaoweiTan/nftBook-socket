#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <sys/socket.h>
#include <unistd.h>
#include <arpa/inet.h>

#define BUFSIZE 2048
#define SERVICE_PORT 9999
#define PKTSIZE 1000
#define MAXFRAME 10
#define MAXPKT 5


int
main(int argc, char **argv)
{
  struct sockaddr_in myaddr;  /* our address */
  struct sockaddr_in remaddr; /* remote address */
  socklen_t addrlen = sizeof(remaddr);    /* length of addresses */
  int recvlen;      /* # bytes received */
  int fd;       /* our socket */
  int msgcnt = 0;     /* count # of messages we received */
  unsigned char buf[BUFSIZE]; /* receive buffer */
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

  
  /* Override the loop, sending the video frames */
  int frame_id = 0;

  char pkt[PKTSIZE];
  memset(pkt, sizeof(pkt), 1);

  while (1) {
    // itoa(frame_id, pkt);
    recvlen = recvfrom(fd, buf, BUFSIZE, 0, (struct sockaddr *)&remaddr, &addrlen);
    if (recvlen > 0) {
      printf("Reciving init packet\n");
    }
    int i = 0;
    printf("Sending a frame\n");

    for (; i < MAXPKT; i = i + 1) {
      // itoa(i, pkt + 4);
      sprintf(pkt, "%d%d", frame_id % 10, i);
      int n = sendto(fd, pkt, strlen(pkt), 0, (struct sockaddr *)&remaddr, addrlen);
      if (n < 0)
        error("ERROR in sendto");
    }
    frame_id = frame_id + 1;
  }


  return 0;
}
