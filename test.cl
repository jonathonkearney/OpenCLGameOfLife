__kernel void addOne(__global int *inArray ) {

    int gid = get_global_id( 0 );
    inArray[0] = 1;
}