#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern void *gc_frame_prev;

// The Gimple Garbage Collector.

//===============================================================//
// The Java Heap data structure.

/*   
      ----------------------------------------------------
      |                        |                         |
      ----------------------------------------------------
      ^\                      /^
      | \<~~~~~~~ size ~~~~~>/ |
    from                       to
 */
struct JavaHeap
{
  int size;       // in bytes, note that this if for semi-heap size
  char *from;     // the "from" space pointer
  char *fromFree; // the next "free" space in the from space
  char *to;       // the "to" space pointer
  char *toStart;  // "start" address in the "to" space
  char *toNext;   // "next" free space pointer in the to space
};

// The Java heap, which is initialized by the following
// "heap_init" function.
struct JavaHeap heap;

// Lab 4, exercise 10:
// Given the heap size (in bytes), allocate a Java heap
// in the C heap, initialize the relevant fields.
void Tiger_heap_init(int heapSize)
{
  // You should write 7 statement here:
  // #1: allocate a chunk of memory of size "heapSize" using "malloc"
  void *heap_memo = (void *)malloc(heapSize);
  // #2: initialize the "size" field, note that "size" field
  // is for semi-heap, but "heapSize" is for the whole heap.
  heap.size = heapSize / 2;
  // #3: initialize the "from" field (with what value?)
  heap.from = heap_memo;
  // #4: initialize the "fromFree" field (with what value?)
  heap.fromFree = heap_memo;
  // #5: initialize the "to" field (with what value?)
  heap.to = heap_memo + heapSize / 2;
  // #6: initizlize the "toStart" field with NULL;
  heap.toStart = heap_memo + heapSize / 2;
  // #7: initialize the "toNext" field with NULL;
  heap.toNext = heap_memo + heapSize / 2;
  return;
}

// The "prev" pointer, pointing to the top frame on the GC stack.
// (see part A of Lab 4)
//void *prev = 0;

//===============================================================//
// Object Model And allocation

// Lab 4: exercise 11:
// "new" a new object, do necessary initializations, and
// return the pointer (reference).
/*    ----------------
      | vptr      ---|----> (points to the virtual method table)
      |--------------|
      | isObjOrArray | (0: for normal objects)
      |--------------|
      | length       | (this field should be empty for normal objects)
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| v_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | v_{size-1}   | /e
      ----------------/
*/
// Try to allocate an object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1;
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new(void *vtable, int size)
{
  int total_size = size + 16;
  if (heap.fromFree + total_size > heap.to)
    Tiger_gc();
  if (heap.fromFree + total_size > heap.to)
  {
    printf("OutOfMemory!\n");
    exit(1);
  }
  else
  {
    void *obj = heap.fromFree;
    memset(heap.fromFree, 0, total_size);
    memcpy(heap.fromFree, &vtable, 8);
    *(int *)(heap.fromFree + 8) = 0;
    *(int *)(heap.fromFree + 12) = size;
    heap.fromFree += total_size;
    return obj;
  }
}

// "new" an array of size "length", do necessary
// initializations. And each array comes with an

// extra "header" storing the array length and other information.
/*    ----------------
      | vptr         | (this field should be empty for an array)
      |--------------|
      | isObjOrArray | (1: for array)
      |--------------|
      | length       |
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| e_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | e_{length-1} | /e
      ----------------/
*/
// Try to allocate an array object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this array object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1;
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new_array(int length)
{
  // Your code here:
  int total_size = length * sizeof(int) + 16;
  if (heap.fromFree + total_size > heap.to)
    Tiger_gc();
  if (heap.fromFree + total_size > heap.to)
  {
    printf("OutOfMemory!\n");
    exit(1);
  }
  else
  {
    void *array = heap.fromFree;
    memset(heap.fromFree, 0, total_size);
    *(int *)(heap.fromFree + 8) = 1;
    *(int *)(heap.fromFree + 12) = length;
    heap.fromFree += total_size;
    return array;
  }
}

//===============================================================//
// The Gimple Garbage Collector

// Lab 4, exercise 12:
// A copying collector based-on Cheney's algorithm.
static int Tiger_gc()
{
  // Your code here:
  int garbage_collect_size = 0;
  void *gc_frame = gc_frame_prev; //The gc_frame currently traversed to
  while (gc_frame != 0)
  {
    /* code */
    double item_num = *(double *)gc_frame; //how many obj_model in this gc_frame
    void *ref_var = (void *)gc_frame + 16; //obj_models begin address
    for (int i = 0; i < item_num; i++)
    {
      void *obj_model = (void *)(ref_var + i * 8); //current obj_model in traversed in this frame
      void *forward = (void *)(obj_model + 16);    //Avoid secondary copying
      if (forward < heap.fromFree && forward > heap.from)
      {
        int copy_length;
        if (*(int *)(obj_model + 8) == 0) //normal obj
          copy_length = *(int *)(obj_model + 12) + 24;
        else //array obj
          copy_length = *(int *)(obj_model + 12) * sizeof(int) + 24;
        memcpy(heap.toNext, obj_model, copy_length);
        forward = heap.toNext;
        heap.toNext += copy_length;
      }
    }
    gc_frame = (void *)(gc_frame + 8);
  }
  void *pre_from = heap.from;
  garbage_collect_size = (heap.fromFree - heap.from) - (heap.toNext - heap.toStart);
  heap.from = heap.toStart;
  heap.fromFree = heap.toNext;
  heap.toStart = pre_from;
  heap.toNext = pre_from;
  return garbage_collect_size;
}
