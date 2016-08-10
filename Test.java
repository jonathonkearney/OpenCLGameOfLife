package OpenCL;

import org.jocl.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.jocl.CL.*;
import static org.jocl.CL.CL_TRUE;

public class Test {

    private static final int N = 100;
    private static int current[][] = new int[N][N];


    public static void main(String[] args) {

        CL.setExceptionsEnabled(true);

        /*for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if ((i+j)/2 % 2 == 0)
                current[i][j] = 1;
            }
        }*/
        current[0][10] = 1;
        current[0][11] = 1;
        current[0][12] = 1;

        Test t = new Test();
        t.TestIteration();
    }

    public void TestIteration(){

        int flatUpdate[] = new int[10404]; //what we read and write the intermediary result into, 102*102 rows
        int flatFinalUpdate[] = new int[10404];
        //flatten the array with 0's around the outside
        int count = 0;
        for (int i = 0; i < 102; i++) {
            for (int j = 0; j < 102; j++) {
                if (i==0||i==101||j==0||j==101){
                    flatUpdate[count] = 0;
                }
                else{
                    flatUpdate[count] = current[i-1][j-1];
                }
                count++;
            }
        }

        for (int i = 0; i < N; i++) {
            System.out.print(flatUpdate[i+103] + ", "); // reads first row
        }
        //pointers
        final Pointer updatePointer = Pointer.to( flatUpdate ); // point to flattened update array
        final Pointer finalPointer = Pointer.to( flatFinalUpdate );

        final int numOfPlatforms[] = new int[ 1 ];
        clGetPlatformIDs( 0, null, numOfPlatforms );
        System.out.println( "Platforms: " + numOfPlatforms[ 0 ] );

        // Grab the platforms
        final cl_platform_id platforms[] = new cl_platform_id[ numOfPlatforms[ 0 ] ];
        clGetPlatformIDs( numOfPlatforms[ 0 ], platforms, null );

        // Use the first platform
        final cl_platform_id platform = platforms[ 0 ];

        // Create the context properties
        final cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty( CL_CONTEXT_PLATFORM, platform );

        // Number of devices
        int numOfDevices[] = new int[ 1 ];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, null, numOfDevices );
        System.out.println( "Devices: " + numOfDevices[ 0 ] );

        // Grab the devices
        final cl_device_id devices[] = new cl_device_id[ numOfDevices[ 0 ] ];
        clGetDeviceIDs( platform, CL_DEVICE_TYPE_ALL, numOfDevices[ 0 ], devices, null );

        // Use the first device
        final cl_device_id device = devices[ 0 ];

        // Create a context for the device
        final cl_context context = clCreateContext( contextProperties, 1, new cl_device_id[]{ device }, null, null, null );

        // Create a command queue for the device
        final cl_command_queue commandQueue = clCreateCommandQueue( context, device, 0, null );

        // Read in the program source
        final String programSource = new BufferedReader( new InputStreamReader(
                main.class.getResourceAsStream( "calculate.cl" )
        ) ).lines().parallel().collect( Collectors.joining("\n") );

        // Create the program from the source code
        final cl_program program = clCreateProgramWithSource( context, 1, new String[]{ programSource }, null, null );
        int buildErr = clBuildProgram( program, 0, null, null, null, null );
        if(buildErr != CL_SUCCESS){
            System.out.println("build error");
        }




        // Create the kernels
        cl_kernel kernels[] = new cl_kernel[3];
        kernels[0] = clCreateKernel( program, "addSideRows", null );
        kernels[1] = clCreateKernel( program, "addSideCols", null );
        kernels[2] = clCreateKernel( program, "calculate", null );

        //allocate memory
        cl_mem memObjects[] = new cl_mem[ 3 ];
        memObjects[ 0 ] = clCreateBuffer( context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int *(N+2)*(N+2), updatePointer, null );
        memObjects[ 1 ] = clCreateBuffer( context, CL_MEM_READ_WRITE, Sizeof.cl_int*(N+2)*(N+2), null, null );
        memObjects[ 2 ] = clCreateBuffer( context, CL_MEM_READ_WRITE, Sizeof.cl_int*(N+2)*(N+2), null, null );

        // Kernel arguments
        int argErr1 = clSetKernelArg(kernels[0], 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        int argErr2 = clSetKernelArg(kernels[0], 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        int argErr3 = clSetKernelArg(kernels[1], 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        int argErr4 = clSetKernelArg(kernels[1], 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        int argErr5 = clSetKernelArg(kernels[2], 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        int argErr6 = clSetKernelArg(kernels[2], 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        int argErr7 = clSetKernelArg(kernels[2], 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        if(argErr1 != CL_SUCCESS || argErr2 != CL_SUCCESS || argErr3 != CL_SUCCESS || argErr4 != CL_SUCCESS){
            System.out.println("argument error");
        }

        // Work item dim
        final long global_work_size[] = new long[]{ N };
        final long local_work_size[] = new long[]{ 1 }; // used to optimize how many work units in work group?

        // Execute the kernel
        int kerr1 = clEnqueueNDRangeKernel(commandQueue, kernels[0], 1, null, global_work_size, local_work_size, 0, null, null);
        int kerr2 = clEnqueueNDRangeKernel(commandQueue, kernels[1], 1, null, global_work_size, local_work_size, 0, null, null);
        int kerr3 = clEnqueueNDRangeKernel(commandQueue, kernels[2], 1, null, global_work_size, local_work_size, 0, null, null);
        if(kerr2 != CL_SUCCESS || kerr1 != CL_SUCCESS){
            System.out.println("execution error");
        }

        //read the buffer. puts the 2nd argument into the 6th?
        int err3 = clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0, (N + 2) * (N + 2) * Sizeof.cl_int, finalPointer, 0, null, null);
        if(err3 != CL_SUCCESS){
            System.out.println("reading error error");
        }

        for (int i = 0; i < N; i++) {
            System.out.print(flatFinalUpdate[i+103] + ", "); // reads first row
        }
        int total = 0;
        for (int i = 0; i < flatFinalUpdate.length; i++) {
            total = total + flatFinalUpdate[i];
        }
        System.out.println("");
        System.out.println("total: " + total);

    }
}
