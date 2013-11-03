#include "syscall.h"

void main()
{

  int file;
  int child;
  int* status;
  char str[8];
  char *c[10];
  char* newfile[10];
  *newfile = "Hellothar";
  file = creat("Test.txt");
  write(file, "testing", 8);
  read(file, str, 8); // testing read and write

  child = exec(*newfile, 1, *c);
  printf("%d\n", child);
  join(child, status); // tests join
  halt();
}


