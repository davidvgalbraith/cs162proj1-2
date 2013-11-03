#include "syscall.h"

int main() {
  int* x;
  int* z;
  char* y[2];
  int e;

  e = exec("jointest.coff", 0, y);
  join(e, &x);
  printf("%d is the &x ", &x);
  printf("%d writtn to x", *(&x));
}
