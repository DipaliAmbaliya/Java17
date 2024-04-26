import static jdk.incubator.foreign.CLinker.*;

import java.io.File;
import java.lang.invoke.*;
import java.util.Arrays;
import java.util.Objects;
import jdk.incubator.foreign.*;

/**
 * Example calls to C library methods from Java/JDK17. 
 * Run on Windows with:
 java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign ForeignTest.java
 * Run on GNU/Linux with:
 java -Djava.library.path=/lib/x86_64-linux-gnu --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign ForeignTest.java
 */
public class ForeignTest {

    private static final CLinker CLINKER = CLinker.getInstance();
    // LOADER is used for symbols from xyz.dll or libxyz.so
    // For every external library dependency add: System.loadLibrary("xyz");
    private static final SymbolLookup LOADER = SymbolLookup.loaderLookup();
    // SYSTEM is used for built-in C runtime library calls.
    private static final SymbolLookup SYSTEM = CLinker.systemLookup();

    static {
        System.out.println("os.name="+System.getProperty("os.name"));
        System.out.println("java.library.path="
                +String.join(System.lineSeparator()+"\t", System.getProperty("java.library.path").split(File.pathSeparator)));
    }
    /** Find native symbol, call System.loadLibrary("xyz") for each dependency */
    static MemoryAddress lookup(String name) {
        return Objects.requireNonNull(LOADER.lookup(name).or(() -> SYSTEM.lookup(name)).get(), () -> "Not found native method: "+name);
    }

    /** Example calls to C runtime library */
    public static void main(String... args) throws Throwable {

        getpid();

        strlen("Hello World");

        printf();

        qsort(0, 9, 33, 45, 3, 4, 6, 5, 1, 8, 2, 7);

    }

    // get a native method handle for 'getpid' function
    private static final MethodHandle GETPID$MH = CLINKER.downcallHandle(
            lookup(System.getProperty("os.name").startsWith("Windows") ? "_getpid":"getpid"),
            MethodType.methodType(int.class),
            FunctionDescriptor.of(CLinker.C_INT));

    private static void getpid() throws Throwable {
        int npid = (int)GETPID$MH.invokeExact();
        System.out.println("getpid() JAVA => "+ProcessHandle.current().pid()+" NATIVE => "+npid);
    }

    private static final MethodHandle STRLEN$MH = CLINKER.downcallHandle(lookup("strlen"),
            MethodType.methodType(long.class, MemoryAddress.class), FunctionDescriptor.of(C_LONG_LONG, C_POINTER));
    public static void strlen(String s) throws Throwable {
        System.out.println("strlen('"+s+"')");

        // strlen(const char *str);
        try(ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            MemorySegment hello = CLinker.toCString(s, allocator);
            long len = (long) STRLEN$MH.invokeExact(hello.address()); // 5
            System.out.println(" => "+len);
        }
    }

    static class Qsort {
        static int qsortCompare(MemoryAddress addr1, MemoryAddress addr2) {
            int v1 = MemoryAccess.getIntAtOffset(MemorySegment.globalNativeSegment(), addr1.toRawLongValue());
            int v2 = MemoryAccess.getIntAtOffset(MemorySegment.globalNativeSegment(), addr2.toRawLongValue());
            return v1 - v2;
        }
    }

    private static final MethodHandle QSORT$MH = CLINKER.downcallHandle(lookup("qsort"),
            MethodType.methodType(void.class, MemoryAddress.class, long.class, long.class, MemoryAddress.class),
            FunctionDescriptor.ofVoid(C_POINTER, C_LONG_LONG, C_LONG_LONG, C_POINTER)
    );

    /**
     * THIS SHOWS DOWNCALL AND UPCALL - uses qsortCompare FROM C code!
     * void qsort(void *base, size_t nitems, size_t size, int (*compar)(const void *, const void*))
     * @param toSort
     */
    public static int[] qsort(int ... toSort) throws Throwable {
        System.out.println("qsort() "+Arrays.toString(toSort));

        MethodHandle comparHandle = MethodHandles.lookup()
                .findStatic(Qsort.class, "qsortCompare",
                        MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class));

        try(ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress comparFunc = CLINKER.upcallStub(
                    comparHandle,FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER),scope
            );

            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            MemorySegment array = allocator.allocateArray(CLinker.C_INT, toSort);
            QSORT$MH.invokeExact(array.address(), (long)toSort.length, 4L, comparFunc.address());
            int[] sorted = array.toIntArray();
            System.out.println(" => "+Arrays.toString(sorted));
            return sorted;
        }
    }

    private static final MethodHandle PRINTF$MH = CLINKER.downcallHandle(lookup("printf"),
            MethodType.methodType(int.class, MemoryAddress.class, int.class, int.class, int.class),
            FunctionDescriptor.of(C_INT,    C_POINTER,           C_INT,    C_INT,    C_INT)
    );
    /** This version hard-codes use of 3 int params as args to the string format */
    public static void printf() throws Throwable {
        System.out.println("printf()");
        int a = 10;
        int b = 7;
        try(ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            MemorySegment s = CLinker.toCString("%d times %d equals %d\n", allocator);
            int rc = (int)PRINTF$MH.invokeExact(s.address(), a, b, a * b);
            System.out.println(" => rc="+rc);
        }
    }


    }