#include "syscall.h"

int main() {
  int* x;
  int* z;
  char* y[2];
  int e;
  // printf("%d is unfancy z", z);
  //printf("%d is the zp ineter ", &z);
  // printf("%d writtn to z", *z);

  //  printf("%d is unfancy x", x);
  //printf("%d is the &x ", &x);
  //printf("%d writtn to x", *x);

  e = exec("emptytest.coff", 0, y);
  join(e, &x);
  printf("%d is the &x ", &x);
  printf("%d writtn to x", *(&x));
}
