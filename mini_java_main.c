// This is automatically generated by the Tiger compiler.
// Do NOT modify!

extern int System_out_println(int);
void* gc_frame_prev;
// structures
struct Sum
{
  struct Sum_vtable *vptr;
  int isObjOrArray;
  int length;
  void* forwarding;
};
struct Doit
{
  struct Doit_vtable *vptr;
  int isObjOrArray;
  int length;
  void* forwarding;
};
// vtables structures
struct Sum_vtable
{
};

struct Doit_vtable
{
  int (*doit)();
};


struct Sum_vtable Sum_vtable_;
struct Doit_vtable Doit_vtable_;

// methods
struct Doit_doit_gc_frame{
    double length;
    void *gc_frame_prev;
    struct Doit* this;
};
int Doit_doit(struct Doit * this, int n)
{
  struct Doit_doit_gc_frame frame;
  frame.length=1;
  frame.gc_frame_prev=gc_frame_prev;
  gc_frame_prev=&frame;
  int sum;
  int i;
  frame.this=this;

  i = 0;
  sum = 0;
  while(i < n)
  {
    {
      sum = sum+i;
      i = i+1;
    }
  }
  gc_frame_prev=frame.gc_frame_prev;
  return sum;
}

// vtables
void vtableInit()
{
    Doit_vtable_.doit=Doit_doit;
}

// main method
struct Tiger_main_gc_frame{
    double length;
    void *gc_frame_prev;
    struct Doit* x_0;
};
int Tiger_main ()
{
  struct Tiger_main_gc_frame frame;
  frame.gc_frame_prev=gc_frame_prev;
  gc_frame_prev=&frame;
  frame.length=1;
  vtableInit();
  struct Doit * x_0;
  System_out_println ((x_0=((struct Doit*)(Tiger_new (&Doit_vtable_, sizeof(struct Doit)))),frame.x_0=x_0, x_0->vptr->doit(x_0, 101)));
  gc_frame_prev=frame.gc_frame_prev;
}




